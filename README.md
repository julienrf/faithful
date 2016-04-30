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

- [![Maven Central](https://img.shields.io/maven-central/v/org.julienrf/faithful_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/org.julienrf/faithful_2.11) `faithful`: provides the `Promise` and `Future` implementations. See [API doc](http://julienrf.github.io/faithful/api/0.1) ;
- [![Maven Central](https://img.shields.io/maven-central/v/org.julienrf/faithful-cats_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/org.julienrf/faithful-cats_2.11) `faithful-cats`: provides instances of [Cats](http://github.com/typelevel/cats) typeclasses for `Future`. See [API doc](http://julienrf.github.io/faithful-cats/api/0.1)

## License

This content is released under the [MIT License](http://opensource.org/licenses/mit-license.php).