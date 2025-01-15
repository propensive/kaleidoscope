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

import language.experimental.captureChecking

enum GlobToken:
  case Star, Globstar, OneChar
  case Exact(char: Char)
  case Range(start: Char, end: Char, inverse: Boolean)
  case Specific(chars: String, inverse: Boolean)

  def regex: String = this match
    case Exact(char) => (if GlobToken.needsEscaping.contains(char) then "\\" else "")+char
    case Star        => "[^/\\\\]*"
    case OneChar     => "[^/\\\\]"
    case Globstar    => ".*"

    case Range(start, end, inverse) =>
      s"[${if inverse then "^" else ""}${Exact(start).regex}-${Exact(end).regex}]"

    case Specific(chars, inverse) =>
      chars.flatMap(Exact(_).regex).mkString(s"[${if inverse then "^" else ""}", "", "]")

object GlobToken:
  private val needsEscaping: Set[Char] = "\\.[]{}()<>*+-=!?^$|".to(Set)
