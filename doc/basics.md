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
