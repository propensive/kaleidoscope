[![Latest version](https://index.scala-lang.org/propensive/kaleidoscope/latest.svg)](https://index.scala-lang.org/propensive/kaleidoscope)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.propensive/kaleidoscope_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.propensive/kaleidoscope_2.12)

# Kaleidoscope

[![Join the chat at https://gitter.im/propensive/kaleidoscope](https://badges.gitter.im/propensive/kaleidoscope.svg)](https://gitter.im/propensive/kaleidoscope?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Availability

An early release of Kaleidoscope is available on Maven Central, and can be
included in an sbt project by including,
```
"com.propensive" %% "kaleidoscope" % "0.1.0"
```
in your `build.sbt` file.

## Usage

To use Kaleidoscope, first import its package,
```scala
import kaleidoscope._
```

and you can then use a Kaleidoscope regular expression—a string prefixed with
the letter `r`—anywhere you can use a pattern in Scala. For example,
```scala
path match {
  case r"/images/.*" => println("image")
  case r"/styles/.*" => println("stylesheet")
  case _ => println("something else")
}
```
or,
```scala
email match {
  case r"^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,6}$$" => Some(email)
  case _ => None
}
```

Such patterns with either match or not, however should they match, it is
possible to extract parts of the matched string using capturing groups. The
pattern syntax is exactly as described in the [Java Standard
Library](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html),
with the exception that a capturing group (enclosed within `(` and `)`) may be
bound to an identifier by prefixing the group with an `@`, and the identifier
to extract to, which in standard Scala syntax, is written either as
`$identifier` or `${identifier}`.

Here is an example:
```scala
path match {
  case r"/images/${img}@(.*)" => Image(img)
  case r"/styles/$styles@(.*)" => Stylesheet(styles)
}
```

or as an extractor on a `val`, like so,
```scala
val r"^[a-z0-9._%+-]+@$domain@([a-z0-9.-]+\.$tld@([a-z]{2,6})$$" = "test@example.com"
> domain: String = "example.com"
> tld: String = "com"
```

In addition, regular expressions will be checked at compile-time, and any
issues will be reported then.

## Limitations

Kaleidoscope currently has no support for optional or repeated capturing
groups, however this will probably be added very soon.

## Disclaimer

This is very experimental software, and should not be considered
production-ready.

## Website

There is currently no website for Kaleidoscope.

## License

Kaleidoscope is made available under the Apache 2.0 license.


