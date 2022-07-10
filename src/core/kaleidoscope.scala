/*
    Kaleidoscope, version 0.4.0. Copyright 2018-22 Jon Pretty, Propensive OÜ.

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

import rudiments.*
import gossamer.*

import scala.quoted.*

import java.util.regex.*
import java.util.concurrent.ConcurrentHashMap

case class Regex(pattern: String)

extension (inline sc: StringContext)
  transparent inline def r: Any = ${Regex.extractor('{sc})}

extension (sc: StringContext)
  def r(args: String*): Regex =
    Regex(sc.parts.zip(args.map(Pattern.quote(_))).map(_+_).mkString+sc.parts.last)

object Regex:
  private val cache: ConcurrentHashMap[String, Pattern] = ConcurrentHashMap()

  given Cuttable[Text, Regex] = (text, regex, limit) =>
    List(text.s.split(regex.pattern, limit).nn.map(_.nn).map(Text(_))*)

  def pattern(p: String): Pattern = cache.computeIfAbsent(p, Pattern.compile(_)).nn

  def extractor(sc: Expr[StringContext])(using Quotes): Expr[Any] =
    import quotes.reflect.*
    val parts = sc.value.get.parts

    def countGroups(part: String): Int =
      val (_, count) = part.tails.to(List).mtwin.map(_.take(1) -> _.slice(1, 3)).foldLeft(false -> 0):
        case ((esc, cnt), (t"(", _)) if esc                  => (false, cnt)
        case ((_, cnt), (t"(", t"?<"))                       => (false, cnt + 1)
        case ((_, cnt), (t"(", opt)) if opt.startsWith(t"?") => (false, cnt)
        case ((_, cnt), (t"(", _))                           => (false, cnt + 1)
        case ((esc, cnt), (t"\\", _))                        => (!esc, cnt)
        case ((_, cnt), _)                                   => (false, cnt)
      
      count

    val groups: List[Int] = parts.map(countGroups).inits.map(_.sum).to(List).reverse.tail
    val pattern = (parts.head +: parts.tail.map(_.substring(1).nn)).mkString
    
    parts.tail.foreach:
      p =>
        if p.length < 2 || p.charAt(0) != '@' || p.charAt(1) != '('
        then report.errorAndAbort("kaleidoscope: variable must be bound to a capturing group")
    
    try Pattern.compile(pattern) catch case e: PatternSyntaxException =>
      report.errorAndAbort(s"kaleidoscope: ${e.getDescription} in pattern")
    
    if parts.length == 1 then '{Regex.Simple(${Expr(pattern)})}
    else '{Regex.Extract(${Expr(pattern)}, ${Expr(groups)}, ${Expr(parts)})}

  class Extractor(xs: IArray[String]):
    def lengthCompare(len: Int): Int = if xs.length == len then 0 else -1
    def apply(i: Int): String = xs(i)
    def drop(n: Int): scala.Seq[String] = xs.drop(n).to(Seq)
    def toSeq: scala.Seq[String] = xs.to(Seq)

  object NoMatch extends Extractor(IArray()):
    override def lengthCompare(len: Int): Int = -1

  private def extract(pattern: Expr[String], groups: Expr[List[Int]], parts: Expr[Seq[String]],
                          scrutinee: Expr[String])
                     (using Quotes): Expr[Extractor] =
    import quotes.reflect.*

    '{
      val matcher: Matcher = Regex.pattern($pattern).matcher($scrutinee).nn
      
      if matcher.matches()
      then Extractor(IArray.range(1, $groups.size).map:
        i => matcher.group($groups(i - 1) + 1).nn
      )
      else NoMatch
    }

  private def matchOne(pattern: Expr[String], groups: Expr[List[Int]], parts: Expr[Seq[String]],
                           scrutinee: Expr[String])
                      (using Quotes): Expr[Option[String]] =
    import quotes.reflect.*

    '{
      val matcher: Matcher = Regex.pattern($pattern).matcher($scrutinee).nn
      if matcher.matches() then
        val matches = (1 until $groups.length).to(Array).map:
          i => matcher.group($groups(i - 1) + 1)
        
        Some(matches.head.nn)
      else None
    }

  case class Simple(pattern: String):
    def unapply(scrutinee: String): Boolean = Regex.pattern(pattern).matcher(scrutinee).nn.matches

  case class Extract(pattern: String, groups: List[Int], parts: Seq[String]):
    transparent inline def unapplySeq(inline scrutinee: String): Extractor =
      ${Regex.extract('pattern, 'groups, 'parts, 'scrutinee)}
