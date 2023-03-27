/*
    Kaleidoscope, version 0.4.0. Copyright 2018-23 Jon Pretty, Propensive OÜ.

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

import probably.*
import rudiments.*
import gossamer.*
import larceny.*

object Tests extends Suite(t"Kaleidoscope tests"):
  def run(): Unit =
    suite(t"Match tests"):
      test(t"simple match"):
        (t"hello world": @unchecked) match
          case r"hello world" => 1
      .assert(_ == 1)
  
      test(t"basic extractor"):
        (t"hello world": @unchecked) match
          case r"(hello world)" => 2
      .assert(_ == 2)
  
      test(t"extract one word"):
        (t"hello world": @unchecked) match
          case r"$first@(hello) world" => first.show
      .check(_ == t"hello")
  
      test(t"extract a nested capture group"):
        (t"hello world": @unchecked) match
          case r"(($first@(hello)) world)" => first.show
      .assert(_ == t"hello")
  
      test(t"extract words"):
        (t"hello world": @unchecked) match
          case r"$first@(hello) $second@(world)" => List(first, second)
      .assert(_ == List(t"hello", t"world"))
      
      test(t"skipped capture group"):
        (t"hello world": @unchecked) match
          case r"(hello) $second@(world)" => second.show
      .assert(_ == t"world")
  
      test(t"skipped capture group 2"):
        (t"1 2 3 4 5": @unchecked) match
          case r"1 $two@(2) 3 4 5" => two.show
      .assert(_ == t"2")
  
      test(t"nested unbound capture group"):
        (t"anyval": @unchecked) match
          case r"$x@(any(val))" => x.show
      .assert(_ == t"anyval")
      
      test(t"email regex"):
        val r"^$prefix@([a-z0-9._%+-]+)@$domain@([a-z0-9.-]+)\.$tld@([a-z]{2,6})$$" =
            t"test@example.com"
        
        List(prefix, domain, tld)
      .assert(_ == List(t"test", t"example", t"com"))

    suite(t"Compiler tests"): 
      test(t"brackets must be matched"):
        captureCompileErrors:
          t"" match
            case r"hello(world" =>
        .head.message
      .assert(_.contains("kaleidoscope: Unclosed group in pattern"))
      
      test(t"variable must be bound"):
        captureCompileErrors:
          t"" match
            case r"hello${space}world" =>
        .head.message
      .assert(_.contains("kaleidoscope: variable must be bound to a capturing group"))
  