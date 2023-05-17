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

import probably.*
import rudiments.*
import spectacular.*
import gossamer.*
import larceny.*

import unsafeExceptions.canThrowAny

object Tests extends Suite(t"Kaleidoscope tests"):
  def run(): Unit =

    suite(t"Regex tests"):
      import Regexp.Group, Regexp.Quantifier.*, Regexp.Greediness.*

      suite(t"Standard parsing"):
        test(t"Parse aaa")(Regexp.parse(List(t"aaa"))).assert(_ == Regexp(t"aaa", Nil))
        
        test(t"Parse (aaa)")(Regexp.parse(List(t"(aaa)"))).assert:
          _ == Regexp(t"(aaa)", List(Group(1, 4, 5)))
        
        test(t"Parse (aa)bb")(Regexp.parse(List(t"(aa)bb"))).assert:
          _ == Regexp(t"(aa)bb", List(Group(1, 3, 4)))
        
        test(t"Parse aa(bb)")(Regexp.parse(List(t"aa(bb)"))).assert:
          _ == Regexp(t"aa(bb)", List(Group(3, 5, 6)))
        
        test(t"Parse aa(bb)cc")(Regexp.parse(List(t"aa(bb)cc"))).assert:
          _ == Regexp(t"aa(bb)cc", List(Group(3, 5, 6)))
        
        test(t"Parse aa(bb)+?cc")(Regexp.parse(List(t"aa(bb)+?cc"))).assert:
          _ == Regexp(t"aa(bb)+?cc", List(Group(3, 5, 8, Nil, AtLeast(1), Reluctant)))
        
        test(t"Parse aa(bb)++cc")(Regexp.parse(List(t"aa(bb)++cc"))).assert:
          _ == Regexp(t"aa(bb)++cc", List(Group(3, 5, 8, Nil, AtLeast(1), Possessive)))
        
        test(t"Parse aa(bb)cc(dd)ee")(Regexp.parse(List(t"aa(bb)cc(dd)ee"))).assert:
          _ == Regexp(t"aa(bb)cc(dd)ee", List(Group(3, 5, 6), Group(9, 11, 12)))
        
        test(t"Parse aa(bb(cc)dd)ee")(Regexp.parse(List(t"aa(bb(cc)dd)ee"))).assert:
          _ == Regexp(t"aa(bb(cc)dd)ee", List(Group(3, 11, 12, List(Group(6, 8, 9)))))
        
        test(t"Parse aa(bb)*cc(dd)ee")(Regexp.parse(List(t"aa(bb)*cc(dd)ee"))).assert:
          _ == Regexp(t"aa(bb)*cc(dd)ee", List(Group(3, 5, 7, Nil, AtLeast(0)), Group(10, 12, 13)))
        
        test(t"Parse aa(bb(cc)*dd)ee"):
          Regexp.parse(List(t"aa(bb(cc)*dd)ee"))
        .assert:
          _ == Regexp(t"aa(bb(cc)*dd)ee", List(Group(3, 12, 13, List(Group(6, 8, 10, Nil, AtLeast(0))))))
        
        test(t"Parse aa(bb)+cc(dd)ee"):
          Regexp.parse(List(t"aa(bb)+cc(dd)ee"))
        .assert:
          _ == Regexp(t"aa(bb)+cc(dd)ee", List(Group(3, 5, 7, Nil, AtLeast(1)), Group(10, 12, 13)))
        
        test(t"Parse aa(bb){4}cc(dd)ee"):
          Regexp.parse(List(t"aa(bb){4}cc(dd)ee"))
        .assert:
          _ == Regexp(t"aa(bb){4}cc(dd)ee", List(Group(3, 5, 9, Nil, Exactly(4)), Group(12, 14, 15)))
        
        test(t"Parse aa(bb){4,}cc(dd)ee"):
          Regexp.parse(List(t"aa(bb){4,}cc(dd)ee"))
        .assert:
          _ == Regexp(t"aa(bb){4,}cc(dd)ee", List(Group(3, 5, 10, Nil, AtLeast(4)), Group(13, 15, 16)))
        
        test(t"Parse aa(bb){4,6}cc(dd)ee"):
          Regexp.parse(List(t"aa(bb){4,6}cc(dd)ee"))
        .assert:
          _ == Regexp(t"aa(bb){4,6}cc(dd)ee", List(Group(3, 5, 11, Nil, Between(4, 6)), Group(14, 16, 17)))
        
        test(t"Parse aa(bb){14,16}ccddee"):
          Regexp.parse(List(t"aa(bb){14,16}ccddee"))
        .assert(_ == Regexp(t"aa(bb){14,16}ccddee", List(Group(3, 5, 13, Nil, Between(14, 16)))))
      
      suite(t"Parsing failures"):
        test(t"Fail to parse aa(bb){14,16ccddee"):
          capture(Regexp.parse(List(t"aa(bb){14,16ccddee")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aa(bb){14!}ccddee"):
          capture(Regexp.parse(List(t"aa(bb){14!}ccddee")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aa(bb{14}ccddee"):
          capture(Regexp.parse(List(t"aa(bb{14}ccddee")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aa(bb){2,1}c"):
          capture(Regexp.parse(List(t"aa(bb){2,1}c")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aabb){2,1}c"):
          capture(Regexp.parse(List(t"aabb){2,1}c")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aa(bb){2,,1}c"):
          capture(Regexp.parse(List(t"aabb){2,,1}c")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aabb){2,,1}c"):
          capture(Regexp.parse(List(t"aabb){2,,1}c")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aabb){,2}c"):
          capture(Regexp.parse(List(t"aabb){,2}c")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aa(bb){2,,1}c"):
          capture(Regexp.parse(List(t"aa(bb){2,,1}c")))
        .assert(_ == InvalidRegexError())
        
        test(t"Fail to parse aa(bb){"):
          capture(Regexp.parse(List(t"aa(bb){")))
        .assert(_ == InvalidRegexError())
      
      suite(t"Test captures"):
        test(t"Capture without parens should fail"):
          capture(Regexp.parse(List(t"a", t"a(bb)")))
        .assert(_ == InvalidRegexError())
        
        test(t"Check simple group is captured"):
          Regexp.parse(List(t"aa", t"(bb)cc"))
        .assert(_ == Regexp(t"aa(bb)cc", List(Group(3, 5, 6, Nil, Exactly(1), Greedy, true))))
        
        test(t"Check simple group at start is captured"):
          Regexp.parse(List(t"", t"(bb)cc"))
        .assert(_ == Regexp(t"(bb)cc", List(Group(1, 3, 4, Nil, Exactly(1), Greedy, true))))
        
        test(t"Check nested group is captured"):
          Regexp.parse(List(t"a(a", t"(bb)c)c"))
        .assert(_ == Regexp(t"a(a(bb)c)c", List(Group(2, 8, 9, List(Group(4, 6, 7, Nil, Exactly(1), Greedy, true))))))
      
        test(t"Check that capture in repeated group is forbidden"):
          capture(Regexp.parse(List(t"a(a", t"(bb)c)*c")))
        .assert(_ == InvalidRegexError())
        
        test(t"Check that capture in another repeated group is forbidden"):
          capture(Regexp.parse(List(t"a(a", t"(bb)c){2}c")))
        .assert(_ == InvalidRegexError())
      
      suite(t"Capturing patterns"):
        test(t"Show plain capturing pattern"):
          Regexp.parse(List(t"abc")).capturePattern
        .assert(_ == t"abc")
        
        test(t"Show simple capturing pattern"):
          Regexp.parse(List(t"a", t"(bc)")).capturePattern
        .assert(_ == t"a(?<g0>bc)")
        
        test(t"Capturing groups are numbered correctly"):
          Regexp.parse(List(t"(hello) ", t"(world)")).capturePattern
        .assert(_ == t"(hello) (?<g0>world)")
        
        test(t"Show double capturing pattern"):
          Regexp.parse(List(t"a", t"(bc)d", t"(ef)")).capturePattern
        .assert(_ == t"a(?<g0>bc)d(?<g1>ef)")
        
        test(t"Show repeating capturing pattern"):
          Regexp.parse(List(t"a", t"(bc)*")).capturePattern
        .assert(_ == t"a(?<g0>(bc)*)")
        
        test(t"Show exact repeating capturing pattern"):
          Regexp.parse(List(t"a", t"(bc){3}")).capturePattern
        .assert(_ == t"a(?<g0>(bc){3})")
      
      suite(t"Matching patterns"):
        test(t"Simple capture"):
          Regexp.parse(List(t"foo", t"(bar)")).matches(t"foobar").map(_.to(List))
        .assert(_ == Some(List(t"bar")))
        
        test(t"Two captures"):
          Regexp.parse(List(t"foo", t"(bar)", t"(baz)")).matches(t"foobarbaz").map(_.to(List))
        .assert(_ == Some(List(t"bar", t"baz")))
        
        test(t"Two captures, one repeating"):
          Regexp.parse(List(t"foo", t"(bar)", t"(baz)*")).matches(t"foobarbazbaz").map(_.to(List))
        .assert(_ == Some(List(t"bar", List(t"baz", t"baz"))))
        
        test(t"Two captures, both repeating"):
          Regexp.parse(List(t"foo", t"(bar){4}", t"(baz)*")).matches(t"foobarbarbarbarbazbaz").map(_.to(List))
        .assert(_ == Some(List(List(t"bar", t"bar", t"bar", t"bar"), List(t"baz", t"baz"))))
        
        test(t"Two captures, one optional and absent, one repeating"):
          Regexp.parse(List(t"foo", t"(bar)+", t"(baz)?")).matches(t"foobarbar").map(_.to(List))
        .assert(_ == Some(List(List(t"bar", t"bar"), None)))
        
        test(t"Two captures, one optional and present, one repeating"):
          Regexp.parse(List(t"foo", t"(b.r)+", t"(baz)?")).matches(t"fooberbirbaz").map(_.to(List))
        .assert(_ == Some(List(List(t"ber", t"bir"), Some(t"baz"))))
        
        test(t"Nested captures, one optional and present, one repeating"):
          Regexp.parse(List(t"f(oo", t"(b.r)+", t"(baz)?)")).matches(t"fooberbirbaz").map(_.to(List))
        .assert(_ == Some(List(List(t"ber", t"bir"), Some(t"baz"))))
      
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
            t"test@example.com": @unchecked
        
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
  
