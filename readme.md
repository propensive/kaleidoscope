[<img alt="GitHub Workflow" src="https://img.shields.io/github/workflow/status/propensive/kaleidoscope/Build/main?style=for-the-badge" height="24">](https://github.com/propensive/kaleidoscope/actions)
[<img src="https://img.shields.io/maven-central/v/com.propensive/kaleidoscope-core?color=2465cd&style=for-the-badge" height="24">](https://search.maven.org/artifact/com.propensive/kaleidoscope-core)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/v7CjtbnwDq)
<img src="/doc/images/github.png" valign="middle">

# Kaleidoscope

Kaleidoscope is a small library which provides pattern matching using regular
expressions, and extraction of capturing groups into values. In particular,
patterns are written inline, and do not need to be predefined.

## Features

- pattern match strings against regular expressions
- regular expressions can be written inline in patterns
- extraction of capturing groups in patterns
- static verification of regular expression syntax


## Availability

The current latest release of Kaleidoscope is __0.4.0__.

## Getting Started

To use Kaleidoscope, first import its package,
```scala
import kaleidoscope.*
```

and you can then use a Kaleidoscope regular expression—a string prefixed with
the letter `r`—anywhere you can use a pattern in Scala. For example,
```scala
path match
  case r"/images/.*" => println("image")
  case r"/styles/.*" => println("stylesheet")
  case _             => println("something else")
```
or,
```scala
email match
  case r"^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,6}$$" => Some(email)
  case _                                            => None
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
path match
  case r"/images/${img}@(.*)"  => Image(img)
  case r"/styles/$styles@(.*)" => Stylesheet(styles)
```

or as an extractor on a `val`, like so (using the Scala REPL):
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

## Limitations

Kaleidoscope currently has no support for optional or repeated capturing
groups.


## Related Projects

The following _Scala One_ libraries are dependencies of _Kaleidoscope_:

[![Gossamer](https://github.com/propensive/gossamer/raw/main/doc/images/128x128.png)](https://github.com/propensive/gossamer/) &nbsp;

The following _Scala One_ libraries are dependents of _Kaleidoscope_:

[![Harlequin](https://github.com/propensive/harlequin/raw/main/doc/images/128x128.png)](https://github.com/propensive/harlequin/) &nbsp; [![Jovian](https://github.com/propensive/jovian/raw/main/doc/images/128x128.png)](https://github.com/propensive/jovian/) &nbsp;

## Status

Kaleidoscope is classified as __maturescent__. Propensive defines the following five stability levels for open-source projects:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Kaleidoscope is designed to be _small_. Its entire source code currently consists of 116 lines of code.

## Building

Kaleidoscope can be built on Linux or Mac OS with Vex, by running the `irk` script in the root directory:
```sh
./irk
```

This script will download `irk` the first time it is run, start a daemon process, and run the build. Subsequent
invocations will be near-instantaneous.

## Contributing

Contributors to Kaleidoscope are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/kaleidoscope/labels/good%20first%20issue"><img alt="label: good first issue"
src="https://img.shields.io/badge/-good%20first%20issue-67b6d0.svg" valign="middle"></a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Kaleidoscope easier.

Please __do not__ contact project maintainers privately with questions. While it can be tempting to repsond to
such questions, private answers cannot be shared with a wider audience, and it can result in duplication of
effort.

## Author

Kaleidoscope was designed and developed by Jon Pretty, and commercial support and training is available from
[Propensive O&Uuml;](https://propensive.com/).



## Name

Kaleidoscope is named after the optical instrument which shows pretty patterns to its user, while the library also works closely with patterns.

## License

Kaleidoscope is copyright &copy; 2018-22 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
