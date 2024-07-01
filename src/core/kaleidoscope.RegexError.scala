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

import fulminate.*

import RegexError.Reason.*

object RegexError:
  enum Reason:
    case UnclosedGroup, ExpectedGroup, BadRepetition, Uncapturable, UnexpectedChar, NotInGroup,
        IncompleteRepetition, InvalidPattern

  object Reason:
    given Reason is Communicable =
      case UnclosedGroup =>
        m"a capturing group was not closed"

      case ExpectedGroup =>
        m"a capturing group was expected immediately following an extractor"

      case BadRepetition =>
        m"the maximum number of repetitions is less than the minimum"

      case Uncapturable =>
        m"a capturing group inside a repeating group can not be extracted"

      case UnexpectedChar =>
        m"the repetition range contained an unexpected character"

      case NotInGroup =>
        m"a closing parenthesis was found without a corresponding opening parenthesis"

      case IncompleteRepetition =>
        m"the repetition range was not closed"

      case InvalidPattern =>
        m"the pattern was invalid"

case class RegexError(reason: RegexError.Reason)
extends Error(m"the regular expression could not be parsed because $reason")
