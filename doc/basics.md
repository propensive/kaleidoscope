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
possible to extract parts of the matched string using capturing groups. The
pattern syntax is exactly as described in the [Java Standard
Library](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html),
with the exception that a capturing group (enclosed within `(` and `)`) may be
bound to an identifier by placing it, like an interpolated string substitution,
immediately prior to the capturing group, as `$identifier` or `${identifier}`.

Here is an example of using a pattern match against filenames:
```scala
enum FileType:
  case Image(text: Text)
  case Stylesheet(text: Text)

def identify(path: Text): FileType = path match
  case r"/images/${img}(.*)"  => FileType.Image(img)
  case r"/styles/$styles(.*)" => FileType.Stylesheet(styles)
```

Alternatively, as with patterns in general, this can be extracted directly in a
`val` definition.

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
applies to repetition ranges, such as `{3}`, `{2,}` or `{1,9}`.

Note that `{1}` will still extract a `Text` value. The type is determined
statically from the pattern, and not dynamically from the runtime scrutinee.

A capture group may be marked as optional, meaning it can appear either zero or
one times. This will extract a value with the type `Optional[Text]`; that is,
if it present it will be a `Text` value, and if not, it will be `Unset`.

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
to be escaped, with the exception of `$` which should be written as `$$`.

It is still necessary, however, to follow the regular expression escaping
rules, for example, an extractor matching a single opening parenthesis would be
written as `r"\("` or `r"""\("""`.

## Globs

Globs offer a simplified and limited form of regular expression. You can use
these in exactly the same way as a standard regular expresion, using the
`g"..."` interpolator instead.

