/* Kaleidoscope, version 0.1.0. Copyright 2018 Jon Pretty, Propensive Ltd.
 *
 * The primary distribution site is: http://propensive.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package kaleidoscope

import language.experimental.macros
import reflect._, reflect.macros._
import java.util.regex._
import scala.collection.mutable.HashMap

object Macros {
  def unapply(c: whitebox.Context)(pattern: c.Tree): c.Tree = {
    import c.universe._

    val q"$_($_(..$partTrees)).r.$method[..$_](..$args)" = c.macroApplication // "
    val parts = partTrees.map { case lit@Literal(Constant(s: String)) => s }
    val positions = partTrees.map { case lit@Literal(_) => lit.pos }
    
    def parsePart(part: String): Int = part.foldLeft((false, 0)) {
      case ((esc, cnt), '(') => if(esc) (false, cnt) else (false, cnt + 1)
      case ((esc, cnt), '\\') => (!esc, cnt)
      case ((esc, cnt), _) => (false, cnt)
    }._2

    parts.tail.foreach { p =>
      if(p.length < 2 || p.head != '@' || p(1) != '(')
        c.abort(c.enclosingPosition, "kaleidoscope: variable must be bound to a capturing group")
    }
    
    val groups = parts.map(parsePart).inits.map(_.sum).to[List].reverse.tail

    val returnType = if(parts.length == 1) tq"_root_.scala.Boolean"
        else tq"_root_.scala.Option[(..${List.fill(parts.length - 1)(tq"_root_.java.lang.String")})]"
    
    val notMatch = if(parts.length == 1) q"false" else q"_root_.scala.None"
    val groupCalls = (1 until groups.length).map { p => q"m.group(${groups(p)})" }
    def isMatch = if(parts.length == 1) q"true" else q"_root_.scala.Some((..$groupCalls))"

    val pattern = (parts.head :: parts.tail.map(_.tail)).mkString
    
    try Pattern.compile(pattern) catch {
      case e: PatternSyntaxException =>
        def find(xs: List[String], err: Int, str: Int): Position = {
          xs match {
            case Nil => positions(str - 1).withPoint(positions(str - 1).point + err)
            case head :: tail => if(head.length < err) find(tail, err - head.length + 1, str + 1) else find(Nil, err, str + 1)
          }
        }
        
        c.abort(find(parts, e.getIndex, 0), s"kaleidoscope: ${e.getDescription} in pattern")
    }

    q"""new { def unapply(input: _root_.java.lang.String): $returnType = {
        val m = _root_.kaleidoscope.Kaleidoscope.pattern($pattern).matcher(input)
        if(m.matches) ${isMatch} else $notMatch
      } }.unapply(..$args)
    """
  }
}

object `package` {
  implicit class RegexStringContext(sc: StringContext) {
    object r { def unapply(pattern: String): Any = macro Macros.unapply }
  }
}

object Kaleidoscope {
  private[this] val cache: HashMap[String, Pattern] = new HashMap()
  def pattern(p: String): Pattern = synchronized(cache.getOrElseUpdate(p, Pattern.compile(p)))
}
