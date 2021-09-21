/*
    Kaleidoscope, version 0.5.0. Copyright 2018-21 Jon Pretty, Propensive OÜ.

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

object Tests extends Suite("Kaleidoscope tests"):
  def run(using Runner): Unit =
    test("simple match") {
      "hello world" match { case r"hello world" => 1 }
    }.assert(_ == 1)

    test("basic extractor") {
      "hello world" match { case r"(hello world)" => 2 }
    }.assert(_ == 2)

    test("extract one word") {
      "hello world" match { case r"$first@(hello) world" => first: String }
    }.assert(_ == "hello")

    test("extract a nested capture group") {
      "hello world" match { case r"(($first@(hello)) world)" => first: String }
    }.assert(_ == "hello")

    test("extract words") {
    //   "hello world" match
    //     case r"$first@(hello) $second@(world)" => List(first, second)
      throw new Exception("This crashes the compiler")
    }.assert(_ == List("hello", "world"))
    
    test("skipped capture group") {
      "hello world" match { case r"(hello) $second@(world)" => second }
    }.assert(_ == "world")

    test("skipped capture group 2") {
      "1 2 3 4 5" match { case r"1 $two@(2) 3 4 5" => two }
    }.assert(_ == "2")

    test("nested unbound capture group") {
      "anyval" match { case r"$x@(any(val))" => x }
    }.assert(_ == "anyval")
    
    test("email regex") {
    //   val r"^$prefix@([a-z0-9._%+-]+)@$domain@([a-z0-9.-]+)\.$tld@([a-z]{2,6})$$" =
    //       "test@example.com"
      
    //   List(prefix, domain, tld)
      throw new Exception("This crashes the compiler")
    }.assert(_ == List("test", "example", "com"))
