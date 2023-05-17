[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/kaleidoscope/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/kaleidoscope/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/7b6mpF6Qcf)
<img src="/doc/images/github.png" valign="middle">

# Kaleidoscope

__Statically-checked inline matching on regular expressions__

Kaleidoscope is a small library which provides pattern matching using regular
expressions, and extraction of capturing groups into values. In particular,
patterns are written inline, and do not need to be predefined.

## Features

- pattern match strings against regular expressions
- regular expressions can be written inline in patterns
- extraction of capturing groups in patterns
- static verification of regular expression syntax


## Availability

Kaleidoscope has not yet been published as a binary. It is currently waiting for the
final release of Scala 3.3.

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

### Escaping

Note that inside an extractor pattern string, whether it is single- (`r"..."`)
or triple-quoted (`r"""..."""`), special characters, notably `\`, do not need
to be escaped, with the exception of `$` which should be written as `$$`. It is
still necessary, however, to follow the regular expression escaping rules, for
example, an extractor matching a single opening parenthesis would be written as
`r"\("` or `r"""\("""`.

### Limitations

Kaleidoscope currently has no support for optional or repeated capturing
groups.



## Status

Kaleidoscope is classified as __maturescent__. For reference, Scala One projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, are still ready to
be used, but caution should be taken if there is a mismatch between the
project's stability level and the importance of your own project.

Kaleidoscope is designed to be _small_. Its entire source code currently consists
of 379 lines of code.

## Building

Kaleidoscope can be built on Linux or Mac OS with [Fury](/propensive/fury), however
the approach to building is currently in a state of flux, and is likely to
change.

## Contributing

Contributors to Kaleidoscope are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/kaleidoscope/labels/beginner">beginner</a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Kaleidoscope easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Kaleidoscope was designed and developed by Jon Pretty, and commercial support and training is available from
[Propensive O&Uuml;](https://propensive.com/).



## Name

Kaleidoscope is named after the optical instrument which shows pretty patterns to its user, while the library also works closely with patterns.

In general, Scala One project names are always chosen with some rationale, however it is usually
frivolous. Each name is chosen for more for its _uniqueness_ and _intrigue_ than its concision or
catchiness, and there is no bias towards names with positive or "nice" meanings—since many of the
libraries perform some quite unpleasant tasks.

Names should be English words, though many are obscure or archaic, and it should be noted how
willingly English adopts foreign words. Names are generally of Greek or Latin origin, and have
often arrived in English via a romance language.

## License

Kaleidoscope is copyright &copy; 2023 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
