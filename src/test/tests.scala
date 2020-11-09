/*

    Kaleidoscope, version 0.4.0. Copyright 2018-20 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
    compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is
    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and limitations under the License.

*/
package kaleidoscope.test

import probably._
import contextual.examples.scalac._
import contextual.examples.fqt._
import annotation.StaticAnnotation

import kaleidoscope._
import java.time.{LocalDate, LocalTime, LocalDateTime}


object Tests extends Suite("Kaleidoscope tests") {

  def run(test: Runner): Unit = {
    test("simple match") {
      "hello world" match { case r"hello world" => 1 }
    }.assert(_ == 1)

    test("basic extractor") {
      "hello world" match { case r"(hello world)" => 2 }
    }.assert(_ == 2)

    test("extract one word") {
      "hello world" match { case r"$first@(hello) world" => first }
    }.assert(_ == "hello")

    test("extract a nested capture group") {
      "hello world" match { case r"(($first@(hello)) world)" => first }
    }.assert(_ == "hello")

    test("extract words") {
      "hello world" match { case r"$first@(hello) $second@(world)" => List(first, second) }
    }.assert(_ == List("hello", "world"))
    
    test("skipped capture group") {
      "hello world" match { case r"(hello) $second@(world)" => second }
    }.assert(_ == "world")

    test("nested unbound capture group") {
      "ab" match { case r"$x@(a(b))" => x }
    }.assert(_ == "ab")
    
    test("spurious closing parethesis") {
      scalac""" "hello world" match { case r"(    ))" => () } """
    }.assert(_ == TypecheckError("kaleidoscope: Unmatched closing ')' in pattern"))
    
    test("invalid terminal escape") {
      scalac""" "hello world" match { case r"()\" => () } """
    }.assert(_ == TypecheckError("kaleidoscope: Unexpected internal error in pattern"))
    
    test("email regex") {
      val r"^$prefix@([a-z0-9._%+-]+)@$domain@([a-z0-9.-]+)\.$tld@([a-z]{2,6})$$" = "test@example.com"
      List(prefix, domain, tld)
    }.assert(_ == List("test", "example", "com"))
    
    test("BigDecimal match") {
      BigDecimal("3.14159") match { case d"3.14159" => "pi"; case _ => "not pi" }
    }.assert(_ == "pi")
    
    test("BigDecimal non-match") {
      BigDecimal("1.0") match { case d"3.14159" => "pi"; case _ => "not pi" }
    }.assert(_ == "not pi")
    
    test("BigDecimal static failure") {
      scalac"""BigDecimal("3.14159") match { case d"3x14159" => true }"""
    }.assert(_ == TypecheckError("kaleidoscope: this is not a valid BigDecimal"))
    
    test("BigDecimal non-literal extraction failure") {
      scalac"""BigDecimal("3.14159") match { case d"3$${x}14159" => x }"""
    }.assert(_ == TypecheckError("kaleidoscope: only literal extractions are permitted"))
    
    test("BigInt match") {
      BigInt("314159") match { case i"314159" => "yes"; case _ => "no" }
    }.assert(_ == "yes")
    
    test("BigInt non-match") {
      BigInt("10") match { case i"314159" => "yes"; case _ => "no" }
    }.assert(_ == "no")
    
    test("BigInt static failure") {
      scalac"""BigInt("1") match { case i"xyz" => true }"""
    }.assert(_ == TypecheckError("kaleidoscope: this is not a valid BigInt"))
    
    test("BigInt non-literal extraction failure") {
      scalac"""BigInt("314159") match { case i"3$${x}14159" => x }"""
    }.assert(_ == TypecheckError("kaleidoscope: only literal extractions are permitted"))

    test("Parse flags") {
      "foo" match {
        case r"(?i)$c@(foo)" => Option(c: String)
        case _ => None
      }
    }.assert(_ == Some("foo"))

    test("Date match") {
      LocalDate.parse("2017-02-11") match {
        case date"2017-02-11" => 1
        case _ => None
      }
    }.assert(_ == 1)

    test("Time match") {
      LocalTime.parse("12:15:06") match {
        case time"12:15:06" => 1
        case _ => None
      }
    }.assert(_ == 1)

    test("Time match without seconds") {
      LocalTime.parse("12:15") match {
        case time"12:15" => 1
        case _ => None
      }
    }.assert(_ == 1)

    test("DateTime match") {
      LocalDateTime.parse("2007-12-03T10:15:30") match {
        case datetime"2007-12-03T10:15:30" => 1
        case _ => None
      }
    }.assert(_ == 1)

    test("Date invalid") {
      scalac"""LocalDate.parse("2017-02-11") match { case date"12:15:06" => 1 }"""
    }.assert(_ == TypecheckError("kaleidoscope: this is not a valid LocalDate"))

    test("Time invalid") {
      scalac"""LocalTime.parse("12:15:06") match { case time"2017-02-11" => true }"""
    }.assert(_ == TypecheckError("kaleidoscope: this is not a valid LocalTime"))

    test("DateTime invalid") {
      scalac"""
        LocalDateTime.parse("2007-12-03T10:15:30") match { case datetime"2007-12-03 10:15:30" => true }
      """
    }.assert(_ == TypecheckError("kaleidoscope: this is not a valid LocalDateTime"))
  }
}
