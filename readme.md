[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/kaleidoscope/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/kaleidoscope/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/7b6mpF6Qcf)
<img src="/doc/images/github.png" valign="middle">

# Kaleidoscope

__Statically-checked inline matching on regular expressions__

Kaleidoscope is a small library to make pattern matching against strings more pleasant. Regular
expressions can be written directly in patterns, and capturing groups bound directly to variables,
typed according to the group's repetition. Here is an example:
```amok scala
case class Email(user: Text, domain: Text)

email match
  case r"$user([^@]+)@$domain(.*)" => Email(name, domain)
```

## Features

- pattern match strings against regular expressions
- regular expressions can be written inline in patterns
- extraction of capturing groups in patterns
- typed extraction (into `List`s or `Option`s) of variable-length capturing groups
- static verification of regular expression syntax
- simpler "glob" syntax is also provided


## Availability Plan

Kaleidoscope has not yet been published. The medium-term plan is to build Kaleidoscope
with [Fury](https://github.com/propensive/fury) and to publish it as a source build on
[Vent](https://github.com/propensive/vent). This will enable ordinary users to write and build
software which depends on Kaleidoscope.

Subsequently, Kaleidoscope will also be made available as a binary in the Maven
Central repository. This will enable users of other build tools to use it.

For the overeager, curious and impatient, see [building](#building).

## Getting Started

To use Kaleidoscope, first import its package,
```scala
import kaleidoscope.*
```

and you can then use a Kaleidoscope regular expression—a string prefixed with
the letter `r`—anywhere you can use a pattern in Scala. For example,
```scala
import anticipation.Text

def describe(path: Text): Unit =
  path match
    case r"/images/.*" => println("image")
    case r"/styles/.*" => println("stylesheet")
    case _             => println("something else")
```
or,
```scala
import vacuous.{Optional, Unset}

def validate(email: Text): Optional[Text] = email match
  case r"^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,6}$$" => email
  case _                                            => Unset
```

Such patterns will either match or not, however should they match, it is
possible to extract parts of the matched string using capturing groups. The
pattern syntax is exactly as described in the [Java Standard
Library](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html),
with the exception that a capturing group (enclosed within `(` and `)`) may be
bound to an identifier by placing it, like an interpolated string substitution,
immediately prior to the capturing group, as `$identifier` or `${identifier}`.

Here is an example:
```scala
enum FileType:
  case Image(text: Text)
  case Stylesheet(text: Text)

def identify(path: Text): FileType = path match
  case r"/images/${img}(.*)"  => FileType.Image(img)
  case r"/styles/$styles(.*)" => FileType.Stylesheet(styles)
```

Alternatively, this can be extracted directly in a `val` definition, like so:
```scala
val r"^[a-z0-9._%+-]+@$domain([a-z0-9.-]+\.$tld([a-z]{2,6}))$$" =
  "test@example.com": @unchecked
```
In the REPL, this would bind the following values:
```
> domain: Text = t"example.com"
> tld: Text = t"com"
```

In addition, the syntax of the regular expressionwill be checked at compile-time, and any
issues will be reported then.

### Repeated and optional capture groups

A normal, unitary capturing group will extract into a `Text` value. But if a capturing group has
a repetition suffix, such as `*` or `+`, then the extracted type will be a `List[Text]`. This also
applies to repetition ranges, such as `{3}`, `{2,}` or `{1,9}`. Note that `{1}` will still extract
a `Text` value.

A capture group may be marked as optional, meaning it can appear either zero or one times. This
will extract a value with the type `Option[Text]`.

For example, see how `init` is extracted as a `List[Text]`, below:
```scala
import gossamer.{drop, Rtl}

def parseList(): List[Text] = "parsley, sage, rosemary, and thyme" match
  case r"$only([a-z]+)"                      => List(only)
  case r"$first([a-z]+) and $second([a-z]+)" => List(first, second)
  case r"$init([a-z]+, )*and $last([a-z]+)"  => init.map(_.drop(2, Rtl)) :+ last
```

### Escaping

Note that inside an extractor pattern string, whether it is single- (`r"..."`)
or triple-quoted (`r"""..."""`), special characters, notably `\`, do not need
to be escaped, with the exception of `$` which should be written as `$$`. It is
still necessary, however, to follow the regular expression escaping rules, for
example, an extractor matching a single opening parenthesis would be written as
`r"\("` or `r"""\("""`.




## Status

Kaleidoscope is classified as __maturescent__. For reference, Soundness projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, can still be used,
as long as caution is taken to avoid a mismatch between the project's stability
level and the required stability and maintainability of your own project.

Kaleidoscope is designed to be _small_. Its entire source code currently consists
of 532 lines of code.

## Building

Kaleidoscope will ultimately be built by Fury, when it is published. In the
meantime, two possibilities are offered, however they are acknowledged to be
fragile, inadequately tested, and unsuitable for anything more than
experimentation. They are provided only for the necessity of providing _some_
answer to the question, "how can I try Kaleidoscope?".

1. *Copy the sources into your own project*
   
   Read the `fury` file in the repository root to understand Kaleidoscope's build
   structure, dependencies and source location; the file format should be short
   and quite intuitive. Copy the sources into a source directory in your own
   project, then repeat (recursively) for each of the dependencies.

   The sources are compiled against the latest nightly release of Scala 3.
   There should be no problem to compile the project together with all of its
   dependencies in a single compilation.

2. *Build with [Wrath](https://github.com/propensive/wrath/)*

   Wrath is a bootstrapping script for building Kaleidoscope and other projects in
   the absence of a fully-featured build tool. It is designed to read the `fury`
   file in the project directory, and produce a collection of JAR files which can
   be added to a classpath, by compiling the project and all of its dependencies,
   including the Scala compiler itself.
   
   Download the latest version of
   [`wrath`](https://github.com/propensive/wrath/releases/latest), make it
   executable, and add it to your path, for example by copying it to
   `/usr/local/bin/`.

   Clone this repository inside an empty directory, so that the build can
   safely make clones of repositories it depends on as _peers_ of `kaleidoscope`.
   Run `wrath -F` in the repository root. This will download and compile the
   latest version of Scala, as well as all of Kaleidoscope's dependencies.

   If the build was successful, the compiled JAR files can be found in the
   `.wrath/dist` directory.

## Contributing

Contributors to Kaleidoscope are welcome and encouraged. New contributors may like
to look for issues marked
[beginner](https://github.com/propensive/kaleidoscope/labels/beginner).

We suggest that all contributors read the [Contributing
Guide](/contributing.md) to make the process of contributing to Kaleidoscope
easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Kaleidoscope was designed and developed by Jon Pretty, and commercial support and
training on all aspects of Scala 3 is available from [Propensive
O&Uuml;](https://propensive.com/).



## Name

Kaleidoscope is named after the optical instrument which shows pretty patterns to its user, while the library also works closely with patterns.

In general, Soundness project names are always chosen with some rationale,
however it is usually frivolous. Each name is chosen for more for its
_uniqueness_ and _intrigue_ than its concision or catchiness, and there is no
bias towards names with positive or "nice" meanings—since many of the libraries
perform some quite unpleasant tasks.

Names should be English words, though many are obscure or archaic, and it
should be noted how willingly English adopts foreign words. Names are generally
of Greek or Latin origin, and have often arrived in English via a romance
language.

## Logo

The logo is a loose allusion to a hexagonal pattern, which could appear in a kaleidoscope.

## License

Kaleidoscope is copyright &copy; 2024 Jon Pretty & Propensive O&Uuml;, and
is made available under the [Apache 2.0 License](/license.md).

