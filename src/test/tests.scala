/*
    Kaleidoscope, version 0.9.0. Copyright 2018-21 Jon Pretty, Propensive OÜ.

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
import eucalyptus.*

import unsafeExceptions.canThrowAny

given Log(Everything |-> Stdout)

object Tests extends Suite(str"Kaleidoscope tests"):
  def run(using Runner): Unit =
    test(str"simple match") {
      "hello world" match { case r"hello world" => 1 }
    }.assert(_ == 1)

    test(str"basic extractor") {
      str"hello world" match { case r"(hello world)" => 2 }
    }.assert(_ == 2)

    test(str"extract one word") {
      str"hello world" match { case r"$first@(hello) world" => first.show }
    }.check(_ == str"hello")

    test(str"extract a nested capture group") {
      str"hello world" match { case r"(($first@(hello)) world)" => first.show }
    }.check(_ == str"hello")

    test(str"extract words") {
      str"hello world" match
        case r"$first@(hello) $second@(world)" => List(first, second)
    }.assert(_ == List(str"hello", str"world"))
    
    test(str"skipped capture group") {
      str"hello world" match { case r"(hello) $second@(world)" => second.show }
    }.check(_ == str"world")

    test(str"skipped capture group 2") {
      str"1 2 3 4 5" match { case r"1 $two@(2) 3 4 5" => two.show }
    }.check(_ == str"2")

    test(str"nested unbound capture group") {
      str"anyval" match { case r"$x@(any(val))" => x.show }
    }.check(_ == str"anyval")
    
    test(str"email regex") {
      val r"^$prefix@([a-z0-9._%+-]+)@$domain@([a-z0-9.-]+)\.$tld@([a-z]{2,6})$$" =
          str"test@example.com"
      
      List(prefix, domain, tld)
    }.assert(_ == List(str"test", str"example", str"com"))
