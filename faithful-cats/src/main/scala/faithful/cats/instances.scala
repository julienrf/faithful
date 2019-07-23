package faithful.cats

import cats.MonadError
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

  implicit val faithfulFutureMonadErrorWithCoflatMap: MonadError[Future, Throwable] =
    new MonadError[Future, Throwable] {
      def pure[A](a: A): Future[A] = Future.successful(a)
      def raiseError[A](e: Throwable): Future[A] = Future.failed(e)
      override def map[A, B](fa: Future[A])(f: A => B) = {
        val promiseB = new Promise[B]()
        fa(a => promiseB.success(f(a)), e => promiseB.failure(e))
        promiseB.future
      }
      def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = {
        val promiseB = new Promise[B]()
        fa(a => f(a)(b => promiseB.success(b), e => promiseB.failure(e)), e => promiseB.failure(e))
        promiseB.future
      }
      override def handleError[A](fa: Future[A])(f: Throwable => A): Future[A] = {
        val promiseA = new Promise[A]()
        fa(a => promiseA.success(a), error => promiseA.success(f(error)))
        promiseA.future
      }
      def handleErrorWith[A](fa: Future[A])(f: Throwable => Future[A]): Future[A] = {
        val promiseA = new Promise[A]()
        fa(a => promiseA.success(a), error => f(error)(a => promiseA.success(a), e => promiseA.failure(e)))
        promiseA.future
      }
      // Note that this is *not* stack safe.
      def tailRecM[A, B](a: A)(f: A => Future[Either[A, B]]): Future[B] =
        flatMap(f(a)) {
          case Left(b1) => tailRecM(b1)(f)
          case Right(c) => Future.successful(c)
        }
    }

}