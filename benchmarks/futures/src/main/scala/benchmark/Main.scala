package benchmark

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.scalajs.dom.document

object Main extends js.JSApp {

  def sequenceBenchmark(): Future[_] = {
    Future.sequence((1 to 50000).map(Future.successful): Seq[Future[Int]])
  }

  def main() = {

    val startTime = (new js.Date).getTime()
    sequenceBenchmark().map(_ => {
      val endTime = (new js.Date).getTime()
      document.getElementById("result").textContent = (endTime - startTime).toString
    })
    ()
  }

}
