Kaleidoscope is a small library to make pattern matching against strings more pleasant. Regular
expressions can be written directly in patterns, and capturing groups bound directly to variables,
typed according to the group's repetition. Here is an example:
```amok scala
case class Email(user: Text, domain: Text)

email match
  case r"$user([^@]+)@$domain(.*)" => Email(name, domain)
```