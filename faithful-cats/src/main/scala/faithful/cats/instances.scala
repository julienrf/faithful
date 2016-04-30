package faithful.cats

import cats.{CoflatMap, MonadError}
import faithful.{Future, Promise}

/**
  * Provides Cats typeclasses instances for faithful’s `Future[A]`.
  *
  * Example:
  *
  * {{{
  *   import faithful.Future
  *   import faithful.cats.Instances._
  *   import cats.syntax.all._
  *
  *   def foo(i: Int): Future[Int] = …
  *
  *   foo(42).flatMap { i =>
  *     foo(i + 1)
  *   }.handleErrorWith { e =>
  *     println(e)
  *     foo(0)
  *   }
  * }}}
  */
object Instances {

  /**
    *
    */
  implicit val faithfulFutureMonadErrorWithCoflatMap: MonadError[Future, Throwable] with CoflatMap[Future] =
    new MonadError[Future, Throwable] with CoflatMap[Future] {
      def pure[A](a: A) = Future.successful(a)
      def raiseError[A](e: Throwable) = Future.failed(e)
      override def map[A, B](fa: Future[A])(f: A => B) = {
        val promiseB = new Promise[B]()
        fa(a => promiseB.success(f(a)), promiseB.failure)
        promiseB.future
      }
      def flatMap[A, B](fa: Future[A])(f: A => Future[B]) = {
        val promiseB = new Promise[B]()
        fa(a => f(a)(promiseB.success, promiseB.failure), promiseB.failure)
        promiseB.future
      }
      override def handleError[A](fa: Future[A])(f: Throwable => A) = {
        val promiseA = new Promise[A]()
        fa(promiseA.success, error => promiseA.success(f(error)))
        promiseA.future
      }
      def handleErrorWith[A](fa: Future[A])(f: Throwable => Future[A]) = {
        val promiseA = new Promise[A]()
        fa(promiseA.success, error => f(error)(promiseA.success, promiseA.failure))
        promiseA.future
      }
      def coflatMap[A, B](fa: Future[A])(f: Future[A] => B) = Future.successful(f(fa))

    }

}