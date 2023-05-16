package kaleidoscope

import rudiments.*

import java.util.regex.*
import java.util.concurrent.ConcurrentHashMap

case class Regex(pattern: String):
  lazy val javaPattern: Pattern = Regex.cache.computeIfAbsent(pattern, Pattern.compile(_)).nn
  def unapply(text: Text): Boolean = javaPattern.matcher(text.s).nn.matches

object Regex:
  private val cache: ConcurrentHashMap[String, Pattern] = ConcurrentHashMap()

  def pattern(p: String): Pattern = cache.computeIfAbsent(p, Pattern.compile(_)).nn

  class Extractor(xs: IArray[String]):
    def lengthCompare(len: Int): Int = if xs.length == len then 0 else -1
    def apply(i: Int): Text = Text(xs(i))
    def drop(n: Int): scala.Seq[Text] = xs.drop(n).to(Seq).map(Text(_))
    def toSeq: scala.Seq[Text] = xs.to(Seq).map(Text(_))

  object NoMatch extends Extractor(IArray()):
    override def lengthCompare(len: Int): Int = -1

  case class Simple(pattern: String):
    def unapply(scrutinee: String): Boolean = Regex.pattern(pattern).matcher(scrutinee).nn.matches

  case class Extract(pattern: String, groups: List[Int], parts: Seq[String]):
    transparent inline def unapplySeq(inline scrutinee: Text): Extractor =
      ${KaleidoscopeMacros.extract('pattern, 'groups, 'parts, 'scrutinee)}

case class InvalidRegexError() extends Error(ErrorMessage(List(Text("mismatched parentheses")), Nil))

object Regexp:
  private val cache: ConcurrentHashMap[String, Pattern] = ConcurrentHashMap()
  enum Greediness:
    case Greedy, Reluctant, Possessive

    def serialize: Text = this match
      case Greedy     => Text("")
      case Reluctant  => Text("?")
      case Possessive => Text("+")
  
  enum Quantifier:
    case Exactly(n: Int)
    case AtLeast(n: Int)
    case Between(n: Int, m: Int)

    def serialize: Text = this match
      case Exactly(1)    => Text("")
      case Exactly(n)    => Text(s"{$n}")
      case AtLeast(1)    => Text("+")
      case AtLeast(0)    => Text("*")
      case AtLeast(n)    => Text(s"{$n,}")
      case Between(0, 1) => Text("?")
      case Between(n, m) => Text(s"{$n,$m}")
    
    def unitary: Boolean = this == Exactly(1)
 
  case class Group
      (start: Int, end: Int, outerEnd: Int, groups: List[Group] = Nil,
          quantifier: Quantifier = Quantifier.Exactly(1),
          greediness: Greediness = Greediness.Greedy, capture: Boolean = false):
    
    def orderedGroups: List[Group] = groups.flatMap(_.orderedGroups)
    def outerStart: Int = (start - 1).max(0)
    
    def serialize(pattern: Text, idx: Int): Text =
      val subpattern = pattern.s.substring(start, end)
      val groupName = Text(if capture then s"?<g$idx>" else "")
      
      if quantifier.unitary then Text(s"($groupName$subpattern)")
      else Text(s"($groupName($subpattern)${quantifier.serialize}${greediness.serialize})")

  def parse(parts: List[Text]): Regexp throws InvalidRegexError =
    parts match
      case Nil => throw InvalidRegexError()
      case head :: tail => if !tail.forall(_.s.startsWith("(")) then throw InvalidRegexError()
    
    def captures(todo: List[Text], last: Int, done: Set[Int]): Set[Int] = todo match
      case Nil          => done
      case head :: tail => captures(tail, last+head.s.length, done + last)
    
    val captured: Set[Int] =
      if parts.length > 1 then captures(parts.tail, parts.head.s.length, Set()) else Set()
        
    val text: Text = Text(parts.mkString)
    var index: Int = 0
    
    def cur(): Char = if index >= text.s.length then '\u0000' else text.s.charAt(index)
    extension [ValueType](value: ValueType) def adv(): ValueType = value.tap { _ => index += 1 }
    
    def greediness(): Greediness = cur() match
      case '?' => Greediness.Reluctant.adv()
      case '+' => Greediness.Possessive.adv()
      case _   => Greediness.Greedy

    def quantifier(): Quantifier = cur() match
      case '\u0000' => Quantifier.Exactly(1)
      case '*'      => Quantifier.AtLeast(0).adv()
      case '+'      => Quantifier.AtLeast(1).adv()
      case '?'      => Quantifier.Between(0, 1).adv()
      
      case '{' =>
        index += 1
        val n = number(true)
        
        val quantifier = cur() match
          case '}' =>
            Quantifier.Exactly(n)
          
          case ',' =>
            index += 1
            number(false) match
              case 0 => Quantifier.AtLeast(n)
              case m => if m < n then throw InvalidRegexError() else Quantifier.Between(n, m)
          case _ =>
            throw InvalidRegexError()
        
        if cur() != '}' then throw InvalidRegexError() else quantifier.adv()
      
      case _ =>
        Quantifier.Exactly(1)

    @tailrec
    def number(required: Boolean, num: Int = 0, first: Boolean = true): Int = cur() match
      case '\u0000'         => throw InvalidRegexError()
      case ch if ch.isDigit => index += 1 ; number(required, num*10 + (ch - '0').toInt, false)
      case other            => if first && required then throw InvalidRegexError() else num

    def group(start: Int, children: List[Group], top: Boolean): Group =
      cur() match
        case '\u0000' =>
          if !top then throw InvalidRegexError()
          Group(start, index, (index + 1).min(text.s.length), children.reverse,
              Quantifier.Exactly(1), Greediness.Greedy, captured.contains(start - 1))
        case '(' =>
          index += 1
          group(start, group(index, Nil, false) :: children, top)
        case ')' =>
          val end = index
          index += 1
          val quant = quantifier()
          val greed = greediness()
          Group(start, end, index, children.reverse, quant, greed, captured.contains(start - 1))
        case _ =>
          index += 1
          group(start, children, top)
    
    
    val mainGroup = group(0, Nil, true)
    
    def check(groups: List[Group], canCapture: Boolean): Unit =
      groups.foreach: group =>
        if !canCapture && group.capture then throw InvalidRegexError()
        check(group.groups, canCapture && group.quantifier.unitary)
    
    check(mainGroup.groups, true)
    
    Regexp(text, mainGroup.groups)
      

case class Match(results: List[Text | Option[Text] | List[Text]])

case class Regexp(pattern: Text, groups: List[Regexp.Group]):
  lazy val capturePattern: Text =
    def recur(todo: List[Regexp.Group], last: Int, text: Text, idx: Int): Text = todo match
      case Nil =>
        Text(text.s+pattern.s.substring(last).nn)
      
      case head :: tail =>
        val subpattern = head.serialize(pattern, idx)
        val partial = text.s+pattern.s.substring(last, head.outerStart)+subpattern.nn
        recur(tail, head.outerEnd, Text(partial), idx + 1)
    
    recur(groups, 0, Text(""), 0)
  
  private[kaleidoscope] lazy val javaPattern: Pattern =
    Regexp.cache.computeIfAbsent(capturePattern.s, Pattern.compile(_)).nn

  def matches(text: Text): Option[Match] =
    val matcher = javaPattern.matcher(text.s).nn
    
    def recur
        (todo: List[Regexp.Group], matches: List[Text | Option[Text] | List[Text]], idx: Int)
        : List[Text | Option[Text] | List[Text]] = todo match
      case Nil =>
        matches
      
      case group :: tail =>
        val matches2 =
          if group.capture then
            if group.quantifier.unitary then Text(matcher.group(s"g$idx").nn) :: matches
            else
              val matchedText = matcher.group(s"g$idx").nn
              val subpattern = pattern.s.substring(group.start, group.end).nn
              val compiled = Regexp.cache.computeIfAbsent(subpattern, Pattern.compile(_)).nn
              val submatcher = compiled.matcher(matchedText).nn
              var submatches: List[Text] = Nil
              while submatcher.find() do submatches ::= Text(submatcher.toMatchResult.nn.group(0).nn)

              if group.quantifier == Regexp.Quantifier.Between(0, 1) then submatches.headOption :: matches
              else submatches.reverse :: matches
          else matches


        recur(tail, matches2, idx + 1)
    
    if matcher.matches then Some(Match(recur(groups, Nil, 0).reverse)) else None