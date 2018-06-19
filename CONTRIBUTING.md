# Developing Kaleidoscope

Kaleidoscope uses `make` and `bloop` for building.

## The first time

To install `bloop` and the latest version of the Scala compile, run,
```
make install
```

This will check whether bloop is available, and (if necessary) install it, and
will download the latest version of Scala.

## Compiling

In a separate terminal, start a `bloop` server by calling,
```
bloop server
```

To compile Kaleidoscope, run,
```
make compile
```
The first time you run this, it will download source dependencies and generate
configuration files for bloop.

To run the tests, run,
```
make test
```

`make compile-watch` and `make test-watch` will perform each of these actions
continuously.


