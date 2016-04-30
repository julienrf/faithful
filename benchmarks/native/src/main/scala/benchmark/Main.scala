package benchmark

import org.scalajs.dom.document

import scala.scalajs.js
import scala.scalajs.js.|

object Main extends js.JSApp {

  def sequence[A](eventuallyAs: Seq[js.Thenable[A]]): js.Thenable[Seq[A]] =
    eventuallyAs.foldRight[js.Thenable[List[A]]](js.Promise.resolve[List[A]](List.empty[A])) { (eventuallyA, eventuallyAs) =>
      eventuallyA.`then`[List[A]](
        (a: A) => {
          eventuallyAs.`then`[List[A]](
            (as: List[A]) => (a :: as): (List[A] | js.Thenable[List[A]]),
            js.undefined
          ): (List[A] | js.Thenable[List[A]])
        },
        js.undefined
      )
    }

  def sequenceBenchmark(): js.Thenable[_] = {
    sequence((1 to 50000).map((i: Int) => js.Promise.resolve[Int](i)))
  }

  def main() = {
    val startTime = (new js.Date).getTime()
    sequenceBenchmark().`then`[Unit](_ => {
      val endTime = (new js.Date).getTime()
      document.getElementById("result").textContent = (endTime - startTime).toString
    }, js.undefined)
    ()
  }

}
