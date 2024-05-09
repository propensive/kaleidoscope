/*
    Kaleidoscope, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

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

import language.experimental.captureChecking

import java.util.regex.*
import java.util.concurrent.ConcurrentHashMap

import anticipation.*
import contingency.*
import rudiments.*
import vacuous.*

import RegexError.Reason.*

object Regex:
  private val cache: ConcurrentHashMap[String, Pattern] = ConcurrentHashMap()

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
       capture:    Boolean     = false):

    def outerStart: Int = (start - 1).max(0)
    def allGroups: List[Regex.Group] = groups.flatMap { group => group :: group.allGroups }
    def captureGroups: List[Regex.Group] = allGroups.filter(_.capture)

    def serialize(pattern: Text, index: Int): (Int, Text) =
      val (index2, subpattern) = Regex.makePattern(pattern, groups, start, "".tt, end, index)
      val groupName = (if capture then s"?<g$index>" else "").tt

      if quantifier.unitary then (index2, s"($groupName$subpattern)".tt)
      else (index2, s"($groupName($subpattern)${quantifier.serialize}${greed.serialize})".tt)

  def make(parts: Seq[String])(using Unsafe): Regex =
    import errorHandlers.throwUnsafely
    parse(parts.to(List).map(_.tt))

  def apply(text: Text): Regex raises RegexError = parse(List(text))

  def parse(parts: List[Text]): Regex raises RegexError =
    (parts: @unchecked) match
      case head :: tail =>
        if !tail.all(_.s.startsWith("(")) then abort(RegexError(ExpectedGroup))

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
            number(false) match
              case 0 =>
                Quantifier.AtLeast(n)

              case m =>
                if m < n then abort(RegexError(BadRepetition)) else Quantifier.Between(n, m)

          case _ =>
            abort(RegexError(UnexpectedChar))

        if current() != '}' then abort(RegexError(UnexpectedChar)) else quantifier.adv()

      case _ =>
        Quantifier.Exactly(1)

    @tailrec
    def number(required: Boolean, num: Int = 0, first: Boolean = true): Int = current() match
      case '\u0000' =>
        abort(RegexError(IncompleteRepetition))

      case ch if ch.isDigit =>
        index += 1
        number(required, num*10 + (ch - '0').toInt, false)

      case other =>
        if first && required then abort(RegexError(UnexpectedChar)) else num

    def group(start: Int, children: List[Group], top: Boolean): Group =
      current() match
        case '\u0000' =>
          if !top then abort(RegexError(UnclosedGroup))
          Group(start, index, (index + 1).min(text.s.length), children.reverse,
              Quantifier.Exactly(1), Greed.Greedy, captured.has(start - 1))
        case '(' =>
          index += 1
          group(start, group(index, Nil, false) :: children, top)

        case ')' =>
          if top then abort(RegexError(NotInGroup))
          val end = index
          index += 1
          val quantifier2 = quantifier()
          val greed2 = greed()

          Group(start, end, index, children.reverse, quantifier2, greed2, captured.has(start - 1))

        case _ =>
          index += 1
          group(start, children, top)

    val mainGroup = group(0, Nil, true)

    def check(groups: List[Group], canCapture: Boolean): Unit =
      groups.each: group =>
        if !canCapture && group.capture then abort(RegexError(Uncapturable))
        check(group.groups, canCapture && group.quantifier.unitary)

    check(mainGroup.groups, true)

    Regex(text, mainGroup.groups)

  def makePattern(pattern: Text, todo: List[Group], last: Int, text: Text, end: Int, index: Int)
          : (Int, Text) =

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

  private[kaleidoscope] lazy val javaPattern: Pattern =
    Regex.cache.computeIfAbsent(capturePattern.s, Pattern.compile(_)).nn

  def matches(text: Text): Boolean = !matchGroups(text).isEmpty

  def matchGroups(text: Text): Option[IArray[List[Text] | Optional[Text]]] =
    val matcher = javaPattern.matcher(text.s).nn

    def recur(todo: List[Regex.Group], matches: List[Optional[Text] | List[Text]], index: Int)
            : List[Optional[Text] | List[Text]] =

      todo match
        case Nil =>
          matches

        case group :: tail =>
          val matches2 =
            if group.capture then
              if group.quantifier.unitary then matcher.group(s"g$index").nn.tt :: matches
              else
                val matchedText = matcher.group(s"g$index").nn
                val subpattern = pattern.s.substring(group.start, group.end).nn
                val compiled = Regex.cache.computeIfAbsent(subpattern, Pattern.compile(_)).nn
                val submatcher = compiled.matcher(matchedText).nn
                var submatches: List[Text] = Nil

                while submatcher.find() do submatches ::= submatcher.toMatchResult.nn.group(0).nn.tt

                if group.quantifier == Regex.Quantifier.Between(0, 1)
                then submatches.prim :: matches
                else submatches.reverse :: matches

            else matches

          recur(tail, matches2, index + 1)

    if matcher.matches then Some(IArray.from(recur(captureGroups, Nil, 0).reverse)) else None
