macro implementation for generating an extractor for use in pattern matching

This macro returns a different type of extractor object depending on the number of substitutions in the string.
It must therefore be called in a _transparent inline_ method. The object, however, should never be accessible in
usage, as it will be constructed and applied in a single step within a case pattern.