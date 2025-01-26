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

import scala.collection.mutable as scm

import anticipation.*
import rudiments.*
import vacuous.*

object Scanner:
  given default: DummyImplicit => Scanner = Scanner(true)

class Scanner(var primed: Boolean, var start: Int = 0):
  var lastStart: Int = -1
  var lastEnd: Int = -1
  val matches: scm.HashMap[Regex, Match] = scm.HashMap()
  val offsets: scm.TreeMap[Int, Set[Regex]] = scm.TreeMap()

  type Match = IArray[List[Text] | Optional[Text]]

  def groups[ResultType](regex: Regex, scrutinee: Text, count: Int): ResultType =
    if !primed then
      regex.matchGroups(scrutinee, 0)(using this).foreach: matched =>
        println(s"Found $matched for $regex")
        matches(regex) = matched
        offsets(lastStart) = offsets.getOrElse(lastStart, Set()) + regex

      None.asInstanceOf[ResultType]

    else
      val result = regex.matchGroups(scrutinee, 0)(using this)
      println(result)
      val result2 = result.asInstanceOf[Option[IArray[List[Text] | Optional[Text]]]]

      if count == 2 then result2.map(_.head).asInstanceOf[ResultType]
      else result2.map(Tuple.fromIArray(_)).asInstanceOf[ResultType]

  def apply(regex: Regex, scrutinee: Text): Boolean =
    if !primed then
      //regexes = regex :: regexes
      false
    else start match
      case index: Int =>
        println("Continuing from "+index)
        val matcher = regex.javaPattern.matcher(scrutinee.s).nn
        val found = matcher.find(index)
        if found then start = matcher.start
        found
