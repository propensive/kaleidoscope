package kaleidoscope

case class Glob(pattern: String) {
  def toRegularExpression: String = pattern.foldLeft("") {
    case '*'  => ".*"
    case '?'  => "."
    case c@('.' | '(' | ')' | '+' | '^' | '$' | '@' | '%') => s"\\$C"
    case c    => c.toString
  }
}
