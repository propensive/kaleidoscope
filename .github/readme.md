[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/kaleidoscope/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/kaleidoscope/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.com/invite/MBUrkTgMnA)
<img src="/doc/images/github.png" valign="middle">

# Kaleidoscope

__Statically-typed inline pattern matching on regular expressions__

__Kaleidoscope__ is a small library to make pattern matching against strings more
pleasant. Regular expressions can be written directly in patterns, and
capturing groups bound directly to variables, typed according to the group's
repetition. Here is an example:
```scala
case class Email(user: Text, domain: Text)

email match
  case r"$user([^@]+)@$domain(.*)" => Email(name, domain)
```

Strings are widely used to carry complex data, when it's wiser to use
structured objects. Kaleidoscope makes it easier to move away from strings.

## Features

- pattern match strings against regular expressions
- regular expressions can be written inline in patterns, anywhere a string could match
- direct extraction of capturing groups in patterns
- typed extraction (into `List`s or [Vacuous](https://github.com/propensive/vacuous/) `Optional`s) of variable-length capturing groups
- static checking of regular expression syntax
- simpler "glob" syntax is also provided


## Availability

Kaleidoscope is available as a binary for Scala 3.4.0 and later, from [Maven
Central](https://central.sonatype.com). To include it in an `sbt` build, use
the coordinates:
```scala
libraryDependencies += "dev.soundness" % "kaleidoscope-core" % "0.1.0"
```







## Getting Started

Kaleidoscope is included in the `kaleidoscope` package, and exported to the
`soundness` package.

To use Kaleidoscope alone, you can include the import,
```scala
import kaleidoscope.*
```
or to use it with other [Soundness](https://github.com/propensive/soundness/) libraries, include:
```scala
import soundness.*
```

Note that Kaleidoscope uses the `Text` type from
[Anticipation](https://github.com/propensive/anticipation) and the `Optional`
type from [Vacuous](https://github.com/propensive/vacuous/). These offer some
advantages over `String` and `Option`, and they can be easily converted:
`Text#s` converts a `Text` to a `String` and `Optional#option` converts an
`Optional` value to its equivalent `Option`. The necessary imports are shown in
the examples.

You can then use a Kaleidoscope regular expression—a string prefixed with
the letter `r`—anywhere you can pattern match against a string in Scala. For example,
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
possible to extract parts of the matched string using _capturing groups_. The
pattern syntax is exactly as described in the [Java Standard
Library](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html),
with the exception that a capturing group (enclosed within `(` and `)`) may be
bound to an identifier by placing it, like an interpolated string substitution,
immediately before to the capturing group, as `$identifier` or `${identifier}`.

Here is an example of using a pattern match against filenames:
```scala
enum FileType:
  case Image(text: Text)
  case Stylesheet(text: Text)

def identify(path: Text): FileType = path match
  case r"/images/${img}(.*)"  => FileType.Image(img)
  case r"/styles/$styles(.*)" => FileType.Stylesheet(styles)
```

Alternatively, as with patterns in general, these can be extracted directly in a
`val` definition (though this is common usage).

Here is an example of matching an email address:
```scala
val r"^[a-z0-9._%+-]+@$domain([a-z0-9.-]+\.$tld([a-z]{2,6}))$$" =
  "test@example.com": @unchecked
```

The `@unchecked` annotation ascribed to the result is standard Scala, and
acknowledges to the compiler that the match is _partial_ and may fail at
runtime.

If you try this example in the Scala REPL, it would bind the following values:
```
> domain: Text = t"example.com"
> tld: Text = t"com"
```

In addition, the syntax of the regular expression will be checked at
compile-time, and any issues will be reported then.

### Repeated and optional capture groups

A normal, _unitary_ capturing group, like `domain` and `tld` above, will
extract into `Text` values. But if a capturing group has a repetition suffix,
such as `*` or `+`, then the extracted type will be a `List[Text]`. This also
applies to repetition ranges, such as `{3}` (exactly three times), `{2,}`
(at least twice) or `{1,9}` (at least once, and at most nine times). Note that `{1}`
will still extract a `Text` value, not a `List[Text]`.

The type of each captured group is determined
statically from the pattern, and not dynamically from the runtime scrutinee.

A capture group may be marked as optional, meaning it can appear either zero or
one times. This will extract a value with the type `Optional[Text]`; that is,
if it present it will be a `Text` value, and if not, it will be `Unset`.

For example, see how `init` is extracted as a `List[Text]`, below:
```scala
import gossamer.{skip, Rtl}

def parseList(): List[Text] = "parsley, sage, rosemary, and thyme" match
  case r"$only([a-z]+)"                      => List(only)
  case r"$first([a-z]+) and $second([a-z]+)" => List(first, second)
  case r"$init([a-z]+, )*and $last([a-z]+)"  => init.map(_.skip(2, Rtl)) :+ last
```

### Escaping

Escaping happens at two levels between source code and regular expression.
First when source code is interpreted as a string. And again when that string
is interpreted as a regular expression pattern.

This is particularly apparent when pattern matching a single backslash (`\`) in Java:
we must write `java.util.regex.Pattern.compile("\\\\")`. The backslash in the
regular expression needs to be escaped with another backslash; then _each_ of those
backslashes must be escaped in order to embed it in a string.

The situation is improved in Kaleidoscope patterns, written as single- (`r"..."`)
or triple-quoted (`r"""..."""`) interpolated strings: special characters,
notably `\`, do _not_ need to be escaped. This means that only the regular expression
escaping rules need to be considered.

An exception is `$` (which is used to indicate a substitution) and should be
written as `$$`.

It is still necessary, however, to follow the regular expression escaping
rules, for example, an extractor matching a single opening parenthesis would be
written as `r"\("` or `r"""\("""`.

### `Regex` values

Regular expressions may be defined as values outside of pattern matches, using the
same syntax. For example,
```scala
val IpAddress = r"([0-9]+\.){3}[0-9]+"
```
which may then be used in a pattern,
```scala
input match
  case IpAddress(addr) => addr
```
but can only represent an entire match, and cannot extract capturing groups.

Such values are instances of `Regex` and provide access to the source pattern as a
`Text` value, `Regex#pattern`, as well as the position and nature of any groups
within the pattern.

`Regex`s are used in other Soundness libraries wherever a regular expression is
required. More importantly, `Text` is _never_ used for a value that
represents a regular expression. So it's possible to know from a method's
signature whether its parameter is interpreted as a regular expression or as
a direct string.

Indeed, in Gossamer, the method `sub` (for making substitutions in a textual
value) is overloaded to take either a `Regex` or a `Text` parameter, and to behave
accordingly.

## Globs

Globs offer a simplified but limited form of regular expression. You can use
these in exactly the same way as a standard regular expresion, using the
`g"..."` interpolator instead.

For example,
```scala
path match
  case g"/usr/local/bin/$name"     => name
  case g"/home/*/.local/bin/$name" => name
```

The appearance of a `*` in a glob will match any sequence of characters,
except for `/` and `\`. A `?` will match exactly one character. An extractor,
such as `$name` above, is equivalent to `*`, but binds the value to an identifier.

As an expression (rather than a pattern), and interpolated string, `g""`, will
also produce a `Regex` value, and can be used anywhere a `Regex` is valid. In
fact, globs are implemented as a simpler front-end to regular expressions. So it
would be possible to write, `path.sub(g"/home/*/.local", t"/usr/local")` _or_
`path.sub(r"/home/[^/]*/.local", t"/usr/local")` to achieve the same goal.

### Equality

It is even possible, sometimes, to equate regular expressions and globs. For
example, `g".local/*" == r"\.local/[^/\\]*"` returns `true` because they are
represented by identical underlying patterns. However, the inequality of two
`Regex` instances does not necessarily indicate any difference in the behavior
of two `Regex`s: it may be impossible to find any input where one matches and the
other does not, while they are implemented differently. (As a trivial example,
consider `r"[xy]"` and `r"[yx]"`: non-equal `Regex`s, with identical behavior.)


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
of 681 lines of code.

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

Kaleidoscope is copyright &copy; 2025 Jon Pretty & Propensive O&Uuml;, and
is made available under the [Apache 2.0 License](/license.md).

