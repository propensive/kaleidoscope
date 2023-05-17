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

extension (inline ctx: StringContext)
  transparent inline def r: Any = ${KaleidoscopeMacros.extractor('ctx)}

// extension (sc: StringContext)
//   def r(args: String*): Regex =
//     Regex(sc.parts.zip(args.map(Pattern.quote(_))).map(_+_).mkString+sc.parts.last)

class NoExtraction(pattern: String):
  def unapply(scrutinee: Text): Boolean =
    !Regexp.unsafeParse(List(pattern)).matches(scrutinee).isEmpty

class Extractor[ResultType](parts: Seq[String]):
  def unapply(scrutinee: Text): ResultType =
    val result = Regexp.unsafeParse(parts).matches(scrutinee)
    if parts.length == 2 then result.map(_.head).asInstanceOf[ResultType]
    else result.map(Tuple.fromIArray(_)).asInstanceOf[ResultType]

object KaleidoscopeMacros:
  def extractor(sc: Expr[StringContext])(using Quotes): Expr[Any] =
    import quotes.reflect.*
    val initParts = sc.value.get.parts.to(List)
    
    val parts = initParts.head :: initParts.tail.map: elem =>
      if elem.startsWith("@") then elem.substring(1).nn else elem.nn

    val regexp = try Regexp.parse(parts.map(Text(_))) catch case err: InvalidRegexError => err match
      case InvalidRegexError() => fail("invalid regular expression")


    val types = regexp.captureGroups.map(_.quantifier).map:
      case Regexp.Quantifier.Exactly(1)    => TypeRepr.of[Text]
      case Regexp.Quantifier.Between(0, 1) => TypeRepr.of[Option[Text]]
      case _                               => TypeRepr.of[List[Text]]

    lazy val tupleType =
      if types.length == 1 then types.head
      else AppliedType(defn.TupleClass(types.length).info.typeSymbol.typeRef, types)

    if types.length == 0 then '{NoExtraction(${Expr(parts.head)})}
    else (tupleType.asType: @unchecked) match
      case '[resultType] => '{Extractor[Option[resultType]](${Expr(parts)})}