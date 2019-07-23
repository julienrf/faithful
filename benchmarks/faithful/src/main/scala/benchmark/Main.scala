package benchmark

import scala.scalajs.js
import faithful.{Future, Promise}
import org.scalajs.dom.document

object Main {

  def map[A, B](fa: Future[A])(f: A => B): Future[B] = {
    val promiseB = new Promise[B]()
    fa(a => promiseB.success(f(a)), e => promiseB.failure(e))
    promiseB.future
  }

  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = {
    val promiseB = new Promise[B]()
    fa(a => f(a)(b => promiseB.success(b), e => promiseB.failure(e)), e => promiseB.failure(e))
    promiseB.future
  }

  def sequence[A](eventuallyAs: Seq[Future[A]]): Future[Seq[A]] =
    eventuallyAs.foldRight[Future[List[A]]](Future.successful(List.empty[A])) { (eventuallyA, eventuallyAs) =>
      flatMap(eventuallyA) { a =>
        map(eventuallyAs) { as =>
          a :: as
        }
      }
    }

  def sequenceBenchmark(): Future[_] = {
    sequence((1 to 50000).map(Future.successful))
  }

  def main(args: Array[String]): Unit = {
    val startTime = (new js.Date).getTime()
    map(sequenceBenchmark())(_ => {
      val endTime = (new js.Date).getTime()
      document.getElementById("result").textContent = (endTime - startTime).toString
    })
    ()
  }

}
