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

import language.experimental.macros
import reflect._, reflect.macros._
import java.util.regex._
import java.util.concurrent.ConcurrentHashMap

private[kaleidoscope] object Macros {

  private def abort[Ctx <: whitebox.Context](c: Ctx)(msg: String): Nothing =
    c.abort(c.enclosingPosition, s"kaleidoscope: $msg")

  def intUnapply(c: whitebox.Context)(scrutinee: c.Tree): c.Tree = {
    import c.universe._

    val q"$_($_(..$partTrees)).$_.$method[..$_](..$args)" = c.macroApplication
    val parts = partTrees.map { case lit@Literal(Constant(s: String)) => s }

    if(parts.length > 1) abort(c)("only literal extractions are permitted")
    try BigInt(parts.head)
    catch { case e: NumberFormatException => abort(c)("this is not a valid BigInt")
    }

    q"""new {
      def unapply(input: _root_.scala.math.BigInt): _root_.scala.Boolean = {
        input == BigInt(${parts.head})
      }
    }.unapply(..$args)"""
  }
  
  def decimalUnapply(c: whitebox.Context)(scrutinee: c.Tree): c.Tree = {
    import c.universe._

    val q"$_($_(..$partTrees)).$_.$method[..$_](..$args)" = c.macroApplication
    val parts = partTrees.map { case lit@Literal(Constant(s: String)) => s }

    if(parts.length > 1) abort(c)("only literal extractions are permitted")
    try BigDecimal(parts.head)
    catch { case e: NumberFormatException =>
      abort(c)("this is not a valid BigDecimal")
    }

    q"""new {
      def unapply(input: _root_.scala.math.BigDecimal): _root_.scala.Boolean = {
        input == BigDecimal(${parts.head})
      }
    }.unapply(..$args)"""
  }
  
  /** macro implementation to generate the extractor AST for the given pattern */
  def stringUnapply(c: whitebox.Context)(scrutinee: c.Tree): c.Tree = {
    import c.universe._

    val q"$_($_(..$partTrees)).$_.$method[..$_](..$args)" = c.macroApplication
    val parts = partTrees.map { case lit@Literal(Constant(s: String)) => s }
    val positions = partTrees.map { case lit@Literal(_) => lit.pos }
   
    def parsePart(part: String): Int = part.foldLeft((false, 0)) {
      case ((esc, cnt), '(') => if(esc) (false, cnt) else (false, cnt + 1)
      case ((esc, cnt), '\\') => (!esc, cnt)
      case ((esc, cnt), _) => (false, cnt)
    }._2

    parts.tail.foreach { p =>
      if(p.length < 2 || p.head != '@' || p(1) != '(')
        abort(c)("variable must be bound to a capturing group")
    }
    
    val groups = parts.map(parsePart).inits.map(_.sum).to[List].reverse.tail

    val pattern = (parts.head :: parts.tail.map(_.tail)).mkString
    
    def findPosition(xs: List[String], err: Int, str: Int): Position = {
      xs match {
        case Nil => positions(str - 1).withPoint(positions(str - 1).point + err)
        case head :: tail =>
          if(head.length < err) findPosition(tail, err - head.length + 1, str + 1)
          else findPosition(Nil, err, str + 1)
      }
    }
    
    val groupCalls = (1 until groups.length).map { idx =>
      val term = TermName("_"+idx)
      q"""def $term: _root_.java.lang.String = matcher.group(${groups(idx)})"""
    }

    try Pattern.compile(pattern) catch {
      case e: PatternSyntaxException =>
        c.abort(findPosition(parts, e.getIndex, 0), s"kaleidoscope: ${e.getDescription} in pattern")
    }

    def getDef = groups.length match {
      case 2 => q"def get = matcher.group(${groups(1)})"
      case _ => q"def get = this"
    }

    if(groups.length == 1) q"""new {
      def unapply(input: _root_.java.lang.String): _root_.scala.Boolean = {
        val m = _root_.kaleidoscope.Kaleidoscope.pattern($pattern).matcher(input)
        m.matches()
      }
    }.unapply(..$args)"""
    else q"""new {
      class Match(matcher: _root_.java.util.regex.Matcher) {
        
        def isEmpty = !matcher.matches()
        $getDef
        ..$groupCalls
      }
      
      def unapply(input: _root_.java.lang.String): Match = {
        val matcher: _root_.java.util.regex.Matcher =
          _root_.kaleidoscope.Kaleidoscope.pattern($pattern).matcher(input)
        
        new Match(matcher)
      }
    }.unapply(..$args)"""
  }
}

object `package` {
  /** provides the `r` extractor on strings for creating regular expression pattern matches */
  implicit class KaleidoscopeContext(sc: StringContext) {
    object r {
      /** returns an unapply extractor for the pattern described by the string context */
      def unapply(scrutinee: String): Any = macro Macros.stringUnapply
    }

    object regex {
      /** returns an unapply extractor for the pattern described by the string context */
      def unapply(scrutinee: String): Any = macro Macros.stringUnapply
    }

    object d {
      def unapply(scrutinee: BigDecimal): Any = macro Macros.decimalUnapply
    }
    
    object i {
      def unapply(scrutinee: BigInt): Any = macro Macros.intUnapply
    }
  }
}

object Kaleidoscope {
  private[this] val cache: ConcurrentHashMap[String, Pattern] = new ConcurrentHashMap()
 
  /** provides access to the cached compiled `Pattern` object for the given string */
  def pattern(p: String): Pattern = {
    val result = cache.get(p)
    if(result == null) {
      val newPattern = Pattern.compile(p)
      cache.put(p, newPattern)
      newPattern
    } else result
  }
}
