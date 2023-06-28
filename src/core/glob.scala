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

import language.experimental.captureChecking

enum GlobToken:
  case Star, Globstar, OneChar
  case Exact(char: Char)
  case Range(start: Char, end: Char, inverse: Boolean)
  case Specific(chars: String, inverse: Boolean)

  def regex: String = this match
    case Exact(char) => (if GlobToken.needsEscaping.contains(char) then "\\" else "")+char
    case Star        => "[^/\\]*"
    case OneChar     => "[^/\\]"
    case Globstar    => ".*"
    
    case Range(start, end, inverse) =>
      s"[${if inverse then "^" else ""}${Exact(start).regex}-${Exact(end).regex}]"
    
    case Specific(chars, inverse) =>
      chars.flatMap(Exact(_).regex).mkString(s"[${if inverse then "^" else ""}", "", "]")

object GlobToken:
  private val needsEscaping: Set[Char] = "\\.[]{}()<>*+-=!?^$|".to(Set)

case class Glob(tokens: GlobToken*):
  def regex: Text = Text(tokens.flatMap(_.regex).mkString)

object Glob:
  import GlobToken.*
  def parse(text: Text): Glob =

    def range(text: String): GlobToken =
      val inverse = text.startsWith("!")
      val text2 = if inverse then text.drop(1) else text
      if text2.length == 3 && text2(1) == '-' then GlobToken.Range(text2(0), text2(2), inverse)
      else GlobToken.Specific(text2, inverse)
        

    def recur(index: Int, tokens: List[GlobToken]): Glob =
      if index >= text.s.length then Glob(tokens.reverse*) else text.s(index) match
        case '*' => tokens match
          case Star :: tail => recur(index + 1, Globstar :: tail)
          case _            => recur(index + 1, Star :: tokens)
        
        case '?' =>
          recur(index + 1, OneChar :: tokens)
        
        case '[' =>
          val end = text.s.indexOf(']', index + 1)
          recur(end + 1, range(text.s.substring(index + 1, end).nn) :: tokens)
        
        case char =>
          recur(index + 1, Exact(char) :: tokens)
      
    recur(0, Nil)


