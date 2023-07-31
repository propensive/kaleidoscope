/*
    Kaleidoscope, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package kaleidoscope

import anticipation.*
import rudiments.*

import java.util.regex.*
import java.util.concurrent.ConcurrentHashMap

import language.experimental.captureChecking

object RegexError:
  enum Reason:
    case UnclosedGroup, ExpectedGroup, BadRepetition, Uncapturable, UnexpectedChar, NotInGroup,
        IncompleteRepetition, InvalidPattern
  
  object Reason:
    given AsMessage[Reason] =
      case UnclosedGroup =>
        msg"a capturing group was not closed"
    
      case ExpectedGroup =>
        msg"a capturing group was expected immediately following an extractor"
    
      case BadRepetition =>
        msg"the maximum number of repetitions is less than the minimum"
    
      case Uncapturable =>
        msg"a capturing group inside a repeating group can not be extracted"
    
      case UnexpectedChar =>
        msg"the repetition range contained an unexpected character"
    
      case NotInGroup =>
        msg"a closing parenthesis was found without a corresponding opening parenthesis"

      case IncompleteRepetition =>
        msg"the repetition range was not closed"
      
      case InvalidPattern =>
        msg"the pattern was invalid"

case class RegexError(reason: RegexError.Reason)
extends Error(msg"the regular expression could not be parsed because $reason")

import RegexError.Reason.*

object Regex:
  private val cache: ConcurrentHashMap[String, Pattern] = ConcurrentHashMap()
  enum Greed:
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
          quantifier: Quantifier = Quantifier.Exactly(1), greed: Greed = Greed.Greedy,
          capture: Boolean = false):
    
    def outerStart: Int = (start - 1).max(0)
    def allGroups: List[Regex.Group] = groups.flatMap { group => group :: group.allGroups }
    def captureGroups: List[Regex.Group] = allGroups.filter(_.capture)
    
    def serialize(pattern: Text, index: Int): (Int, Text) =
      val (index2, subpattern) = Regex.makePattern(pattern, groups, start, Text(""), end, index)
      val groupName = Text(if capture then s"?<g$index>" else "")
      
      if quantifier.unitary then (index2, Text(s"($groupName$subpattern)"))
      else (index2, Text(s"($groupName($subpattern)${quantifier.serialize}${greed.serialize})"))

  def unsafeParse(parts: Seq[String]): Regex =
    try parse(parts.to(List).map(Text(_))) catch case _: RegexError => ???
  
  def apply(text: Text): Regex throws RegexError = parse(List(text))

  def parse(parts: List[Text]): Regex throws RegexError =
    (parts: @unchecked) match
      case head :: tail =>
        if !tail.forall(_.s.startsWith("(")) then throw RegexError(ExpectedGroup)
    
    def captures(todo: List[Text], last: Int, done: Set[Int]): Set[Int] = todo match
      case Nil          => done
      case head :: tail => captures(tail, last+head.s.length, done + last)
    
    val captured: Set[Int] =
      if parts.length > 1 then captures(parts.tail, parts.head.s.length, Set()) else Set()
        
    val text: Text = Text(parts.mkString)
    var index: Int = 0
    
    def cur(): Char = if index >= text.s.length then '\u0000' else text.s.charAt(index)
    extension [ValueType](value: ValueType) def adv(): ValueType = value.tap { _ => index += 1 }
    
    def greed(): Greed = cur() match
      case '?' => Greed.Reluctant.adv()
      case '+' => Greed.Possessive.adv()
      case _   => Greed.Greedy

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
              case 0 =>
                Quantifier.AtLeast(n)
              
              case m =>
                if m < n then throw RegexError(BadRepetition) else Quantifier.Between(n, m)
          
          case _ =>
            throw RegexError(UnexpectedChar)
        
        if cur() != '}' then throw RegexError(UnexpectedChar) else quantifier.adv()
      
      case _ =>
        Quantifier.Exactly(1)

    @tailrec
    def number(required: Boolean, num: Int = 0, first: Boolean = true): Int = cur() match
      case '\u0000' =>
        throw RegexError(IncompleteRepetition)
      
      case ch if ch.isDigit =>
        index += 1
        number(required, num*10 + (ch - '0').toInt, false)
      
      case other =>
        if first && required then throw RegexError(UnexpectedChar) else num

    def group(start: Int, children: List[Group], top: Boolean): Group =
      cur() match
        case '\u0000' =>
          if !top then throw RegexError(UnclosedGroup)
          Group(start, index, (index + 1).min(text.s.length), children.reverse,
              Quantifier.Exactly(1), Greed.Greedy, captured.contains(start - 1))
        case '(' =>
          index += 1
          group(start, group(index, Nil, false) :: children, top)
        case ')' =>
          if top then throw RegexError(NotInGroup)
          val end = index
          index += 1
          val quant = quantifier()
          val greed2 = greed()
          Group(start, end, index, children.reverse, quant, greed2, captured.contains(start - 1))
        case _ =>
          index += 1
          group(start, children, top)
    
    
    val mainGroup = group(0, Nil, true)
    
    def check(groups: List[Group], canCapture: Boolean): Unit =
      groups.foreach: group =>
        if !canCapture && group.capture then throw RegexError(Uncapturable)
        check(group.groups, canCapture && group.quantifier.unitary)
    
    check(mainGroup.groups, true)
    
    Regex(text, mainGroup.groups)
      
  def makePattern
      (pattern: Text, todo: List[Regex.Group], last: Int, text: Text, end: Int, index: Int)
      : (Int, Text) =
    todo match
      case Nil =>
        (index, Text(text.s+pattern.s.substring(last, end).nn))
      
      case head :: tail =>
        val (index2, subpattern) = head.serialize(pattern, index)
        val partial = text.s+pattern.s.substring(last, head.outerStart)+subpattern.nn
        val index3 = if head.capture then index2 + 1 else index2
        
        makePattern(pattern, tail, head.outerEnd, Text(partial), end, index3)

case class Regex(pattern: Text, groups: List[Regex.Group]):
  def unapply(text: Text): Boolean = text.s.matches(pattern.s)

  lazy val capturePattern: Text =
    Regex.makePattern(pattern, groups, 0, Text(""), pattern.s.length, 0)(1)
  
  def allGroups: List[Regex.Group] = groups.flatMap { group => group :: group.allGroups }
  def captureGroups: List[Regex.Group] = allGroups.filter(_.capture)

  private[kaleidoscope] lazy val javaPattern: Pattern =
    Regex.cache.computeIfAbsent(capturePattern.s, Pattern.compile(_)).nn

  def matches(text: Text): Boolean = !matchGroups(text).isEmpty

  def matchGroups(text: Text): Option[IArray[Text | List[Text] | Option[Text]]] =
    val matcher = javaPattern.matcher(text.s).nn
    
    def recur
        (todo: List[Regex.Group], matches: List[Text | Option[Text] | List[Text]], index: Int)
        : List[Text | Option[Text] | List[Text]] = todo match
      case Nil =>
        matches
      
      case group :: tail =>
        val matches2 =
          if group.capture then
            if group.quantifier.unitary then Text(matcher.group(s"g$index").nn) :: matches
            else
              val matchedText = matcher.group(s"g$index").nn
              val subpattern = pattern.s.substring(group.start, group.end).nn
              val compiled = Regex.cache.computeIfAbsent(subpattern, Pattern.compile(_)).nn
              val submatcher = compiled.matcher(matchedText).nn
              var submatches: List[Text] = Nil
              
              while submatcher.find()
              do submatches ::= Text(submatcher.toMatchResult.nn.group(0).nn)

              if group.quantifier == Regex.Quantifier.Between(0, 1)
              then submatches.headOption :: matches
              else submatches.reverse :: matches
          
          else matches

        recur(tail, matches2, index + 1)
    
    if matcher.matches then Some(IArray.from(recur(captureGroups, Nil, 0).reverse)) else None
