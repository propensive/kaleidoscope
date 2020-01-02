/*

  Kaleidoscope, version 0.2.0. Copyright 2018 Jon Pretty, Propensive Ltd.

  The primary distribution site is: https://propensive.com/

  Licensed under the Apache License, Version 2.0 (the "License"); you may not use
  this file except in compliance with the License. You may obtain a copy of the
  License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations under
  the License.

*/
package kaleidoscope

import contextual._
import java.time.{LocalDate, LocalTime, LocalDateTime}
import java.time.format.{DateTimeFormatter, DateTimeParseException}

object datetime {
  object DateParser extends Verifier[LocalDate] {
    def check(string: String): Either[(Int, String), LocalDate] = {
      try Right(LocalDate.parse(string))
      catch { case e: DateTimeParseException => Left((0, "could not parse date")) }
    }
  }

  object TimeParser extends Verifier[LocalTime] {
    def check(string: String): Either[(Int, String), LocalTime] = {
      try Right(LocalTime.parse(string))
      catch { case e: DateTimeParseException => Left((0, "could not parse time")) }
    }
  }

  object DateTimeParser extends Verifier[LocalDateTime] {
    def check(string: String): Either[(Int, String), LocalDateTime] = {
      try Right(LocalDateTime.parse(string))
      catch { case e: DateTimeParseException => Left((0, "could not parse datetime")) }
    }
  }

  implicit class DateStringContext(sc: StringContext) { val date = Prefix(DateParser, sc) }
  implicit class TimeStringContext(sc: StringContext) { val time = Prefix(TimeParser, sc) }
  implicit class DateTimeStringContext(sc: StringContext) { val datetime = Prefix(DateTimeParser, sc) }
}
