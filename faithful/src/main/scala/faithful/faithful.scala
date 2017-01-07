package faithful

import scala.scalajs.js

/**
  * An object which can be completed with a value or failed with an exception.
  */
final class Promise[A]() {

  import Future.{failed, successful}

  private val pending: js.Array[(js.Function1[A, _], js.Function1[Throwable, _])] = js.Array()

  private var completion: Future[A] = null

  /**
    * Attempts to successfully complete this promise with `a`
    * @throws Error if this promise has already been resolved
    */
  def success(a: A): Unit = complete(successful(a))

  /**
    * Attempts to complete this promise with `error`
    * @throws Error if this promise has already been resolved
    */
  def failure(error: Throwable): Unit = complete(failed(error))

  private def complete(completion: Future[A]): Unit = {
    if (this.completion == null) {
      this.completion = completion
      var i: Int = 0
      val l = pending.length
      while (i < l) {
        val (onSuccess, onError) = pending(i)
        this.completion(onSuccess, onError)
        i += 1
      }
    } else throw new Error("This promise has already been completed")
  }

  /**
    * @return a `Future[A]` that will eventually be resolved to this promise’s value
    */
  val future: Future[A] = (onSuccess, onError) => {
    if (completion == null) {
      pending.push((onSuccess, onError))
    } else {
      completion(onSuccess, onError)
    }
  }

}

object `package` {

  /**
    * `Future[A]` is only defined as a type alias for `((success: A => _), failure: js.Any => _) => _`.
    *
    * So, it is just a function that accepts two functions. The former will eventually
    * be called when the `A` value is resolved. The latter will eventually be
    * called to signal an error.
    */
  type Future[+A] = js.Function2[js.Function1[A, _], js.Function1[Throwable, _], _]

}

/**
  * Provides convenient shorthands to define `Future` values.
  */
object Future {

  /**
    * @return A `Future[A]` that immediately resolves to `a`
    */
  def successful[A](a: A): Future[A] = (onSuccess, onError) => onSuccess(a)

  /**
    * @return A `Future[Nothing]` that immediately fails with `error`
    */
  def failed(error: Throwable): Future[Nothing] = (onSuccess, onError) => onError(error)

  /**
    * @return The completion state of the given future (`None` if not yet completed)
    */
  def completion[A](fa: Future[A]): Option[Either[Throwable, A]] = {
    var result: Option[Either[Throwable, A]] = None
    fa(a => result = Some(Right(a)), error => result = Some(Left(error)))
    result
  }

}