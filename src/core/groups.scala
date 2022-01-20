/*
    Kaleidoscope, version 0.9.0. Copyright 2018-22 Jon Pretty, Propensive OÜ.

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

import java.util.regex.*

case class CaptureGroup(start: Int, end: Int, min: Int, max: Int, bound: Boolean)

def parsePattern(pattern: String): Matcher =
  def recur(idx: Int, stack: List[CaptureGroup], groups: List[CaptureGroup], binding: Boolean = false): Matcher =
    if idx == pattern.length then Matcher(pattern, groups)
    else
      pattern(idx) match
        case '(' => recur(idx + 1, CaptureGroup(idx + 1, idx + 1, min = 1, max = 1, binding) :: stack, groups)
        case ')' => recur(idx + 1, stack.tail, stack.head.copy(end = idx) :: groups)
        case '*' => if groups.headOption.fold(false)(_.end == idx - 1) then recur(idx + 1, stack, groups.head.copy(min = 0, max = Int.MaxValue) :: groups.tail) else recur(idx + 1, stack, groups)
        case '+' => if groups.headOption.fold(false)(_.end == idx - 1) then recur(idx + 1, stack, groups.head.copy(min = 1, max = Int.MaxValue) :: groups.tail) else recur(idx + 1, stack, groups)
        case '?' => if groups.headOption.fold(false)(_.end == idx - 1) then recur(idx + 1, stack, groups.head.copy(min = 0, max = 1) :: groups.tail) else recur(idx + 1, stack, groups)
        case ch  => recur(idx + 1, stack, groups)

  recur(0, Nil, Nil)

@main
def run(p: String, s: String): Unit =
  val pat = parsePattern(p)
  println(pat(s))

case class Matcher(pattern: String, groups: List[CaptureGroup]):
  def apply(full: String): List[List[String]] =
    val pat = Pattern.compile(pattern)
    
    def recur(str: String, last: Int, result: List[List[String]]): List[List[String]] =
      println("matching "+str)
      val m = pat.matcher(str)
      if m.matches
      then
        val g = groups.length - result.length + 1
        val start = m.start(g)
        val end = m.end(g)

        if start != end && (last == -1 || last == end) then
          val newStr = str.substring(0, start)+str.substring(end)
          val matched = str.substring(m.start(g), m.end(g))
        
          val newResult = result match
            case h :: t => (matched :: h) :: t
            case _      => List(List(matched))
          
          if newResult.length < recur(newStr, start, newResult)
        else recur(full, -1, Nil :: result)
        
      else
        if result.length < groups.length
        then recur(full, -1, Nil :: result)
        else result
        
    recur(full, -1, List(Nil))
