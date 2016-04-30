package benchmark

import scala.scalajs.js
import faithful.{Future, Promise}
import org.scalajs.dom.document

object Main extends js.JSApp {

  def map[A, B](fa: Future[A])(f: A => B): Future[B] = {
    val promiseB = new Promise[B]()
    fa(a => promiseB.success(f(a)), promiseB.failure)
    promiseB.future
  }

  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = {
    val promiseB = new Promise[B]()
    fa(a => f(a)(promiseB.success, promiseB.failure), promiseB.failure)
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

  def main() = {
    val startTime = (new js.Date).getTime()
    map(sequenceBenchmark())(_ => {
      val endTime = (new js.Date).getTime()
      document.getElementById("result").textContent = (endTime - startTime).toString
    })
    ()
  }

}
