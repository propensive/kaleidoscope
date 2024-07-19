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

