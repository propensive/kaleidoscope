<a href="https://furore.dev/propensive/kaleidoscope"><img src="/doc/images/furore.png" style="vertical-align:middle" valign="middle"></a>&nbsp;&nbsp;<a href="https://furore.dev/propensive/kaleidoscope">__Develop with Fury__ </a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://riot.im/app/#/room/#propensive.kaleidoscope:matrix.org"><img src="/doc/images/riotim.png" style="vertical-arign:middle" valign="middle"></a>&nbsp;&nbsp;<a href="https://riot.im/app/#/room/#propensive.kaleidoscope:matrix.org">__Discuss on Riot__</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://search.maven.org/search?q=g:com.propensive%20AND%20a:kaleidoscope_2.12"><img src="/doc/images/mavencentral.png" style="vertical-arign:middle" valign="middle"></a>&nbsp;&nbsp;<a href="https://search.maven.org/search?q=g:com.propensive%20AND%20a:kaleidoscope_2.12">__Download from Maven Central__</a>

<img src="/doc/images/github.png" valign="middle">

# Kaleidoscope

Kaleidoscope is a small library which provides pattern matching using regular expressions, and extraction of capturing groups into values. In particular, patterns are written inline, and do not need to be predefined.

## Features



## Getting Started

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

Such patterns will either match or not, however should they match, it is
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

## Escaping

Note that inside an extractor pattern string, whether it is single- (`r"..."`)
or triple-quoted (`r"""..."""`), special characters, notably `\`, do not need
to be escaped, with the exception of `$` which should be written as `$$`. It is
still necessary, however, to follow the regular expression escaping rules, for
example, an extractor matching a single opening parenthesis would be written as
`r"\("` or `r"""\("""`.

### Limitations

Kaleidoscope currently has no support for optional or repeated capturing
groups, however this will probably be added very soon.


## Availability

Kaleidoscope&rsquo;s source is available on GitHub, and may be built with [Fury](https://github.com/propensive/fury) by
cloning the layer `propensive/kaleidoscope`.
```
fury layer clone -i propensive/kaleidoscope
```
or imported into an existing layer with,
```
fury layer import -i propensive/kaleidoscope
```
A binary will be made available on Maven Central.

## Contributing

Contributors to Kaleidoscope are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/kaleidoscope/labels/good%20first%20issue"><img alt="label: good first issue"
src="https://img.shields.io/badge/-good%20first%20issue-67b6d0.svg" valign="middle"></a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Kaleidoscope easier.

Please __do not__ contact project maintainers privately with questions, as other users cannot then benefit from
the answers.

## Author

Kaleidoscope was designed and developed by [Jon Pretty](https://twitter.com/propensive), and commercial support and
training is available from [Propensive O&Uuml;](https://propensive.com/).



## License

Kaleidoscope is copyright &copy; 2018-20 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
