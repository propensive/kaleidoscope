package adversaria.tests

import estrapade.{TestApp, test}
import contextual.data.scalac._
import contextual.data.fqt._
import annotation.StaticAnnotation

import kaleidoscope._


object Tests extends TestApp {

  def tests(): Unit = {
    test("simple match") {
      "hello world" match { case r"hello world" => 1 }
    }.assert(_ == 1)

    test("basic extractor") {
      "hello world" match { case r"(hello world)" => 2 }
    }.assert(_ == 2)

    test("extract words") {
      "hello world" match { case r"$first@(hello) $second@(world)" => List(first, second) }
    }.assert(_ == List("hello", "world"))
    
    test("skipped capture group") {
      "hello world" match { case r"(hello) $second@(world)" => second }
    }.assert(_ == "world")
    
    test("unmatched capturing group") {
      scalac""" "hello world" match { case r"(" => () } """
    }.assert(_ == TypecheckError("kaleidoscope: Unclosed group in pattern"))
    
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
  }
}
