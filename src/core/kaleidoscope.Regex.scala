/*
    Kaleidoscope, version 0.26.0. Copyright 2025 Jon Pretty, Propensive OÜ.

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

import language.experimental.pureFunctions

import java.util.regex as jur
import java.util.concurrent.ConcurrentHashMap

import anticipation.*
import contingency.*
import denominative.*
import proscenium.*
import rudiments.*
import vacuous.*

import RegexError.Reason.*

object Regex:
  private val cache: ConcurrentHashMap[String, jur.Pattern] = ConcurrentHashMap()

  enum Greed:
    case Greedy, Reluctant, Possessive

    def serialize: Text = this match
      case Greedy     => "".tt
      case Reluctant  => "?".tt
      case Possessive => "+".tt

  enum Quantifier:
    case Exactly(n: Int)
    case AtLeast(n: Int)
    case Between(n: Int, m: Int)

    def serialize: Text = this match
      case Exactly(1)    => "".tt
      case Exactly(n)    => s"{$n}".tt
      case AtLeast(1)    => "+".tt
      case AtLeast(0)    => "*".tt
      case AtLeast(n)    => s"{$n,}".tt
      case Between(0, 1) => "?".tt
      case Between(n, m) => s"{$n,$m}".tt

    def unitary: Boolean = this == Exactly(1)

  case class Group
     (start:      Int,
      end:        Int,
      outerEnd:   Int,
      groups:     List[Group] = Nil,
      quantifier: Quantifier  = Quantifier.Exactly(1),
      greed:      Greed       = Greed.Greedy,
      capture:    Boolean     = false,
      charClass:  Boolean     = false):

    def outerStart: Int = (start - 1).max(0)
    def allGroups: List[Regex.Group] = groups.flatMap { group => group :: group.allGroups }
    def captureGroups: List[Regex.Group] = allGroups.filter(_.capture)

    def serialize(pattern: Text, index: Int): (Int, Text) =
      if charClass then
        val groupName = (if capture then s"?<g$index>" else "").tt
        if quantifier.unitary then (index, s"($groupName[${pattern.s.substring(start, end)}])")
        else
          val chars = pattern.s.substring(start, end)
          (index, s"($groupName[$chars]${quantifier.serialize}${greed.serialize})")
      else
        val (index2, subpattern) = Regex.makePattern(pattern, groups, start, "".tt, end, index)
        val groupName = (if capture then s"?<g$index>" else "").tt

        if quantifier.unitary then (index2, s"($groupName$subpattern)".tt)
        else (index2, s"($groupName($subpattern)${quantifier.serialize}${greed.serialize})".tt)

  def make(parts: Seq[String])(using Unsafe): Regex =
    import strategies.throwUnsafely
    parse(parts.to(List).map(_.tt))

  def apply(text: Text): Regex raises RegexError = parse(List(text))

  def parse(parts: List[Text]): Regex raises RegexError =
    parts.absolve match
      case head :: tail =>
        if !tail.all { part => part.s.startsWith("(") || part.s.startsWith("[") }
        then abort(RegexError(0, ExpectedGroup))

    def captures(todo: List[Text], last: Int, done: Set[Int]): Set[Int] = todo match
      case Nil          => done
      case head :: tail => captures(tail, last+head.s.length, done + last)

    val captured: Set[Int] =
      if parts.length > 1 then captures(parts.tail, parts.head.s.length, Set()) else Set()

    val text: Text = parts.mkString.tt
    var index: Int = 0

    def current(): Char = if index >= text.s.length then '\u0000' else text.s.charAt(index)
    extension [ValueType](value: ValueType) def adv(): ValueType = value.also { index += 1 }

    def greed(): Greed = current() match
      case '?' => Greed.Reluctant.adv()
      case '+' => Greed.Possessive.adv()
      case _   => Greed.Greedy

    def quantifier(): Quantifier = current() match
      case '\u0000' => Quantifier.Exactly(1)
      case '*'      => Quantifier.AtLeast(0).adv()
      case '+'      => Quantifier.AtLeast(1).adv()
      case '?'      => Quantifier.Between(0, 1).adv()

      case '{' =>
        index += 1
        val n = number(true)

        val quantifier = current() match
          case '}' =>
            Quantifier.Exactly(n)

          case ',' =>
            index += 1
            if current() == '}' then Quantifier.AtLeast(n)
            else number(false) match
              case 0 =>
                abort(RegexError(index - 1, ZeroMaximum))

              case m =>
                if m < n then abort(RegexError(index - 1, BadRepetition))
                else Quantifier.Between(n, m)

          case _ =>
            abort(RegexError(index, UnexpectedChar))

        if current() != '}' then abort(RegexError(index, UnexpectedChar)) else quantifier.adv()

      case _ =>
        Quantifier.Exactly(1)

    @tailrec
    def number(required: Boolean, num: Int = 0, first: Boolean = true): Int = current() match
      case '\u0000' =>
        abort(RegexError(index, IncompleteRepetition))

      case ch if ch.isDigit =>
        index += 1
        number(required, num*10 + (ch - '0').toInt, false)

      case ',' =>
        if !required then abort(RegexError(index, UnexpectedChar)) else num

      case '}' =>
        if first && required then abort(RegexError(index, UnexpectedChar)) else num

      case other =>
        abort(RegexError(index, UnexpectedChar))

    def group
       (start: Int, children: List[Group], top: Boolean, escape: Boolean, charClass: Boolean)
    :     Group =

      current() match
        case '\u0000' =>
          if !top then abort(RegexError(index, UnclosedGroup))

          Group(start, index, (index + 1).min(text.s.length), children.reverse,
              Quantifier.Exactly(1), Greed.Greedy, captured.has(start - 1), false)

        case '\\' =>
          index += 1
          group(start, children, top, !escape, charClass)

        case char if escape =>
          index += 1
          group(start, children, top, false, charClass)

        case '[' if !charClass =>
          index += 1
          group(start, group(index, Nil, false, false, true) :: children, top, false, false)

        case ']' if charClass =>
          if index - 1 == start then abort(RegexError(index, EmptyCharClass))
          index += 1
          if top then abort(RegexError(index - 1, NotInGroup))
          val end = index - 1
          val quantifier2 = quantifier()
          val greed2 = greed()

          Group(start, end, index, Nil, quantifier2, greed2, captured.has(start - 1), true)

        case char if charClass =>
          index += 1
          group(start, children, top, false, charClass)

        case '(' =>
          index += 1
          group(start, group(index, Nil, false, false, false) :: children, top, false, false)

        case ')' =>
          index += 1
          if top then abort(RegexError(index - 1, NotInGroup))
          val end = index - 1
          val quantifier2 = quantifier()
          val greed2 = greed()

          Group
           (start,
            end,
            index,
            children.reverse,
            quantifier2,
            greed2,
            captured.has(start - 1),
            false)

        case _ =>
          index += 1
          group(start, children, top, false, charClass)

    val mainGroup = group(0, Nil, true, false, false)

    def check(groups: List[Group], canCapture: Boolean): Unit =
      groups.each: group =>
        if !canCapture && group.capture then abort(RegexError(group.start - 1, Uncapturable))
        check(group.groups, canCapture && group.quantifier.unitary)

    check(mainGroup.groups, true)

    Regex(text, mainGroup.groups)

  def makePattern(pattern: Text, todo: List[Group], last: Int, text: Text, end: Int, index: Int)
  :     (Int, Text) =

    todo match
      case Nil =>
        (index, (text.s+pattern.s.substring(last, end).nn).tt)

      case head :: tail =>
        val (index2, subpattern) = head.serialize(pattern, index)
        val partial = text.s+pattern.s.substring(last, head.outerStart)+subpattern.nn
        val index3 = if head.capture then index2 + 1 else index2

        makePattern(pattern, tail, head.outerEnd, partial.tt, end, index3)

case class Regex(pattern: Text, groups: List[Regex.Group]):
  def unapply(text: Text): Boolean = text.s.matches(pattern.s)

  lazy val capturePattern: Text =
    Regex.makePattern(pattern, groups, 0, "".tt, pattern.s.length, 0)(1)

  def allGroups: List[Regex.Group] = groups.flatMap { group => group :: group.allGroups }
  def captureGroups: List[Regex.Group] = allGroups.filter(_.capture)

  private[kaleidoscope] lazy val javaPattern: jur.Pattern =
    Regex.cache.computeIfAbsent(capturePattern.s, jur.Pattern.compile(_)).nn

  def seek(input: Text, start: Ordinal = Prim): Optional[Interval] =
    val matcher: jur.Matcher = javaPattern.matcher(input.s).nn
    if matcher.find(start.n0) then Interval.zerary(matcher.start, matcher.end) else Unset


  def search(input: Text, start: Ordinal = Prim, overlap: Boolean = false): Stream[Interval] =
    val matcher: jur.Matcher = javaPattern.matcher(input.s).nn

    def recur(offset: Int): Stream[Interval] =
      if matcher.find(offset)
      then
        Interval.zerary(matcher.start, matcher.end)
        #:: recur((if overlap then matcher.start else matcher.end) + 1)
      else Stream()

    recur(start.n0)

  def matches(text: Text)(using Scanner): Boolean = !matchGroups(text).isEmpty

  def matchGroups(text: Text)(using scanner: Scanner)
  :     Option[IArray[List[Text | Char] | Optional[Text | Char]]] =

    val matcher: jur.Matcher = javaPattern.matcher(text.s).nn

    def recur
       (todo:    List[Regex.Group],
        matches: List[Optional[Text | Char] | List[Text | Char]],
        index:   Int)
    :     List[Optional[Text | Char] | List[Text | Char]] =

      todo match
        case Nil =>
          matches

        case group :: tail =>
          val matchedText = matcher.group(s"g$index").nn
          val matches2 =
            if group.capture then
              if group.charClass then
                if group.quantifier.unitary then matchedText.head :: matches
                else if group.quantifier == Regex.Quantifier.Between(0, 1)
                then matchedText.headOption.getOrElse(Unset) :: matches
                else matchedText.toCharArray.nn.to(List) :: matches
              else

              if group.quantifier.unitary then matcher.group(s"g$index").nn.tt  :: matches
              else
                if group.charClass then matchedText.toCharArray.nn.to(List) :: matches else
                  val subpattern = pattern.s.substring(group.start, group.end).nn
                  val compiled = Regex.cache.computeIfAbsent(subpattern, jur.Pattern.compile(_)).nn
                  val submatcher = compiled.matcher(matchedText).nn
                  var submatches: List[Text] = Nil

                  while submatcher.find()
                  do submatches ::= submatcher.toMatchResult.nn.group(0).nn.tt

                  if group.quantifier == Regex.Quantifier.Between(0, 1)
                  then submatches.prim :: matches
                  else submatches.reverse :: matches

            else matches

          recur(tail, matches2, index + 1)

    scanner.nextStart match
      case Unset =>
        if !matcher.matches then None else Some(IArray.from(recur(captureGroups, Nil, 0).reverse))

      case index: Int =>
        if !matcher.find(index) then None else
          scanner.nextStart = matcher.start + 1
          Some(IArray.from(recur(captureGroups, Nil, 0).reverse))

def extract[ValueType](input: Text, start: Ordinal = Prim)
   (lambda: Scanner ?=> PartialFunction[Text, ValueType]): Stream[ValueType] =
  if start.n0 < input.s.length then
    val scanner = Scanner(start.n0)
    lambda(using scanner).lift(input) match
      case Some(head) => head #:: extract(input, Ordinal.zerary(scanner.nextStart.or(0)))(lambda)
      case _          => Stream()
  else Stream()
