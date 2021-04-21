/*

    Kaleidoscope, version 0.5.0. Copyright 2018-20 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
    compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is
    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and limitations under the License.

*/
package kaleidoscope

import java.util.regex.*
import java.util.concurrent.ConcurrentHashMap

import scala.quoted.*

extension (inline sc: StringContext)
  transparent inline def r: Any = ${Regex.extractor('{sc})}

object Regex:
  private val cache: ConcurrentHashMap[String, Pattern] = ConcurrentHashMap()
  
  def pattern(p: String): Pattern = cache.computeIfAbsent(p, Pattern.compile)

  def extractor(sc: Expr[StringContext])(using quotes: Quotes): Expr[Any] =
    import quotes.reflect.*
    val parts = sc.value.get.parts

    def countGroups(part: String): Int =
      val (_, count) = part.tails.map { tail => tail.take(1) -> tail.take(3).drop(1) }.foldLeft((false, 0)) {
        case ((esc, cnt), ("(", _)) if esc                               => (false, cnt)
        case ((_, cnt), ("(", "?<"))                                     => (false, cnt + 1)
        case ((_, cnt), ("(", maybeGroup)) if maybeGroup.startsWith("?") => (false, cnt)
        case ((_, cnt), ("(", _))                                        => (false, cnt + 1)
        case ((esc, cnt), ("\\", _))                                     => (!esc, cnt)
        case ((_, cnt), _)                                               => (false, cnt)
      }
      
      count

    val groups: List[Int] = parts.map(countGroups).inits.map(_.sum).to(List).reverse.tail
    val pattern = (parts.head +: parts.tail.map(_.tail)).mkString
    
    parts.tail.foreach { p =>
      if p.length < 2 || p.head != '@' || p(1) != '('
      then report.error("kaleidoscope: variable must be bound to a capturing group")
    }
    
    try Pattern.compile(pattern)
    catch case e: PatternSyntaxException => report.error(s"kaleidoscope: ${e.getDescription} in pattern")
    
    if parts.length == 1 then '{Regex.Simple(${Expr(pattern)})}
    else '{Regex.Extract(${Expr(pattern)}, ${Expr(groups)}, ${Expr(parts)})}

  class Extractor(xs: IArray[String]):
    def lengthCompare(len: Int): Int = if xs.length == len then 0 else -1
    def apply(i: Int): String = xs(i)
    def drop(n: Int): scala.Seq[String] = xs.drop(n).to(Seq)
    def toSeq: scala.Seq[String] = xs.to(Seq)

  private def extract(pattern: Expr[String],
                      groups: Expr[List[Int]],
                      parts: Expr[Seq[String]],
                      scrutinee: Expr[String])
                     (using quotes: Quotes): Expr[Extractor] =
    import quotes.reflect.*

    '{
      val matcher = Regex.pattern($pattern).matcher($scrutinee)
      if matcher.matches() then
        Extractor(IArray.range(1, $groups.length).map { idx => matcher.group($groups(idx - 1) + 1) })
      else null
    }

  private def matchOne(pattern: Expr[String],
                      groups: Expr[List[Int]],
                      parts: Expr[Seq[String]],
                      scrutinee: Expr[String])
                     (using quotes: Quotes): Expr[Option[String]] =
    import quotes.reflect.*

    '{
      val matcher = Regex.pattern($pattern).matcher($scrutinee)
      if matcher.matches() then
        val matches = (1 until $groups.length).to(Array).map { idx => matcher.group($groups(idx - 1) + 1) }
        Some(matches.head)
      else None
    }
  case class Simple(pattern: String):
    def unapply(scrutinee: String): Boolean = Regex.pattern(pattern).matcher(scrutinee).matches

  case class Extract(pattern: String, groups: List[Int], parts: Seq[String]):
    transparent inline def unapplySeq(inline scrutinee: String): Extractor =
      ${Regex.extract('pattern, 'groups, 'parts, 'scrutinee)}