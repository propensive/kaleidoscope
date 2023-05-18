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

import rudiments.*

import scala.quoted.*

import java.util.regex.*

extension (inline ctx: StringContext)
  transparent inline def r: Any = ${KaleidoscopeMacros.extractor('ctx)}

extension (ctx: StringContext)
  def r(args: String*): Regex throws InvalidRegexError =
    Regex.parse(List(Text(ctx.parts.zip(args.map(Pattern.quote(_))).map(_+_).mkString+
        ctx.parts.last)))

class NoExtraction(pattern: String):
  def unapply(scrutinee: Text): Boolean =
    !Regex.unsafeParse(List(pattern)).matches(scrutinee).isEmpty

class Extractor[ResultType](parts: Seq[String]):
  def unapply(scrutinee: Text): ResultType =
    val result = Regex.unsafeParse(parts).matches(scrutinee)
    if parts.length == 2 then result.map(_.head).asInstanceOf[ResultType]
    else result.map(Tuple.fromIArray(_)).asInstanceOf[ResultType]

object KaleidoscopeMacros:
  def extractor(sc: Expr[StringContext])(using Quotes): Expr[Any] =
    import quotes.reflect.*
    val parts = sc.value.get.parts.to(List)
    
    val regex = try Regex.parse(parts.map(Text(_))) catch case err: InvalidRegexError => err match
      case err@InvalidRegexError(_) => fail(err.message.text.s)

    val types = regex.captureGroups.map(_.quantifier).map:
      case Regex.Quantifier.Exactly(1)    => TypeRepr.of[Text]
      case Regex.Quantifier.Between(0, 1) => TypeRepr.of[Option[Text]]
      case _                               => TypeRepr.of[List[Text]]

    lazy val tupleType =
      if types.length == 1 then types.head
      else AppliedType(defn.TupleClass(types.length).info.typeSymbol.typeRef, types)

    if types.length == 0 then '{NoExtraction(${Expr(parts.head)})}
    else (tupleType.asType: @unchecked) match
      case '[resultType] => '{Extractor[Option[resultType]](${Expr(parts)})}
