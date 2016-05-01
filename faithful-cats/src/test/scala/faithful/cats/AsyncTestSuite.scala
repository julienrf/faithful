package faithful.cats

import faithful.{Future, Promise}
import org.scalatest.AsyncFunSuite
import ScalaFutureConverter.asScalaFuture
import Instances._
import cats.MonadError
import cats.syntax.all._

class AsyncTestSuite extends AsyncFunSuite {

  test("pending and non-pending callbacks") {
    val x = new Promise[Int]()
    val y = x.future.map(_ + 1)
    x.success(42)
    val z = x.future.map(_ * 2)
    val assertion = (y |@| z).map((xx, yy) => assert(xx == 43 && yy == 84))
    asScalaFuture(assertion)
  }

  test("no exception catching") {
    class MyException extends Exception
    val x = new Promise[Int]()
    val y = x.future.map[Int](_ => throw new MyException)
    intercept[MyException] {
      x.success(42)
    }
    succeed
  }

  test("failures") {
    val x = new Promise[Int]()
    val y =
      MonadError[Future, Throwable].handleError(x.future)(_ => 42)
    x.failure(new Exception("Oops"))
    val assertion = y.map(yy => assert(yy == 42))
    asScalaFuture(assertion)
  }

}

object ScalaFutureConverter {

  def asScalaFuture[A](fa: Future[A]): scala.concurrent.Future[A] = {
    val scalaPromiseA = scala.concurrent.Promise[A]()
    fa(a => scalaPromiseA.success(a), error => scalaPromiseA.failure(error))
    scalaPromiseA.future
  }

}