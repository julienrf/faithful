# faithful

> **faithful**: deserving trust ; keeping your promises or doing what you are supposed to do.

Minimalistic Scala.js promise/future implementation that does not violate parametricity.

Key features:
- lightweight: 50 SLOC, about 2 kB of uncompressed JavaScript (that’s about 50 times smaller than depending on Scala’s `Future`),
- efficient: 2 times faster than Scala’s `Future` and 5 times faster than JavaScript’s `Promise`,
- parametric: you can safely implement `Monad[faithful.Future]`,
- reasonable: exceptions are not silently swallowed.

## Usage

The following artifacts are published under the organization name `org.julienrf`:

- [faithful](https://index.scala-lang.org/julienrf/faithful/faithful)
- [faithful-cats](https://index.scala-lang.org/julienrf/faithful/faithful-cats)

## License

This content is released under the [MIT License](http://opensource.org/licenses/mit-license.php).