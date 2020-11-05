/*

    Kaleidoscope, version v0.2.1. Copyright 2018-20 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
    compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is
    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and limitations under the License.

*/
package kaleidoscope

import contextual._
import scala.util.matching._

object txt {

  object TxtParser extends Interpolator {

    case class ContextType() extends Context

    type Output = String

    def contextualize(interpolation: StaticInterpolation): Seq[ContextType] = Nil

    def evaluate(interpolation: RuntimeInterpolation): String =
      interpolation.parts.mkString.stripMargin
  }

  implicit class TxtStringContext(sc: StringContext) { val txt = Prefix(TxtParser, sc) }
}
