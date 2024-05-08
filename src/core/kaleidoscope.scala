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

import scala.quoted.*

import anticipation.*
import contingency.*
import fulminate.*
import vacuous.*

extension (inline ctx: StringContext)
  transparent inline def r: Any = ${Kaleidoscope.regex('ctx)}
  transparent inline def g: Any = ${Kaleidoscope.glob('ctx)}

object Kaleidoscope:
  given Realm = realm"kaleidoscope"

  def glob(context: Expr[StringContext])(using Quotes): Expr[Any] =
    val parts = context.value.get.parts.map(Text(_)).map(Glob.parse(_).regex.s).to(List)

    extractor(parts.head :: parts.tail.map("([^/\\\\]*)"+_))

  def regex(context: Expr[StringContext])(using Quotes): Expr[Any] =
    extractor(context.value.get.parts.to(List))

  private def extractor(parts: List[String])(using Quotes): Expr[Any] =
    import quotes.reflect.*

    val regex = failCompilation(Regex.parse(parts.map(Text(_))))

    val types: List[TypeRepr] = regex.captureGroups.map(_.quantifier).map:
      case Regex.Quantifier.Exactly(1)    => TypeRepr.of[Text]
      case Regex.Quantifier.Between(0, 1) => TypeRepr.of[Optional[Text]]
      case _                              => TypeRepr.of[List[Text]]

    lazy val tupleType =
      if types.length == 1 then types.head
      else AppliedType(defn.TupleClass(types.length).info.typeSymbol.typeRef, types)

    try Pattern.compile(parts.mkString) catch case err: PatternSyntaxException =>
      fail(RegexError(RegexError.Reason.InvalidPattern).message)

    if types.length == 0 then '{NoExtraction(${Expr(parts.head)})}
    else (tupleType.asType: @unchecked) match
      case '[resultType] => '{RExtractor[Option[resultType]](${Expr(parts)})}

  class NoExtraction(pattern: String):
    inline def apply(): Regex = Regex.make(List(pattern))(using Unsafe)

    def unapply(scrutinee: Text): Boolean =
      Regex.make(List(pattern))(using Unsafe).matches(scrutinee)

  class RExtractor[ResultType](parts: Seq[String]):
    def unapply(scrutinee: Text): ResultType =
      val result = Regex.make(parts)(using Unsafe).matchGroups(scrutinee)

      val result2 = result.asInstanceOf[Option[IArray[List[Text] | Optional[Text]]]]

      if parts.length == 2 then result2.map(_.head).asInstanceOf[ResultType]
      else result2.map(Tuple.fromIArray(_)).asInstanceOf[ResultType]
