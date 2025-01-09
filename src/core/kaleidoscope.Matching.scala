package kaleidoscope

import vacuous.*

object Matching:
  given (using DummyImplicit) => Matching as default = Matching(Unset)
class Matching(var nextStart: Optional[Int] = Unset)
