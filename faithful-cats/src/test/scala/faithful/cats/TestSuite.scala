package faithful.cats

import cats.Eq
import cats.data.{Xor, XorT}
import faithful.{Future, Promise}
import cats.laws.discipline.{CoflatMapTests, MonadErrorTests}
import cats.laws.discipline.CartesianTests.Isomorphisms
import cats.std.int.intAlgebra
import cats.data.XorT.xorTEq
import cats.data.Xor.xorEq
import cats.std.option.eqOption
import cats.laws.discipline.eq.unitEq
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

// TODO Write tests that don’t need `Eq[Future[_]]` values
class TestSuite extends FunSuite with Discipline {

  import faithful.cats.Instances._
  import ArbitraryFuture._
  import EqFuture._

  implicit val eqXorTFEA: Eq[XorT[Future, Throwable, Int]] = xorTEq[Future, Throwable, Int]
  implicit val eqIntIntInt: Eq[(Int,Int,Int)] = new Eq[(Int, Int, Int)] {
    def eqv(l: (Int, Int, Int), r: (Int, Int, Int)) =
      Eq[Int].eqv(l._1, r._1) &&
        Eq[Int].eqv(l._2, r._2) &&
        Eq[Int].eqv(l._3,r._3)
  }
  implicit val iso: Isomorphisms[Future] = Isomorphisms.invariant[Future]

  checkAll("Future[Int]", MonadErrorTests[Future, Throwable].monadError[Int, Int, Int])
  checkAll("Future[Int]", CoflatMapTests[Future].coflatMap[Int, Int, Int])

}

object ArbitraryFuture {

  def successfulGen[A](implicit arbA: Arbitrary[A]): Gen[Future[A]] =
    arbA.arbitrary.map(Future.successful)

  val failedGen: Gen[Future[Nothing]] = Gen.const(Future.failed(new Exception("failed")))

  def successfulPromiseGen[A](implicit arbA: Arbitrary[A]): Gen[Future[A]] =
    arbA.arbitrary.map { a =>
      val p = new Promise[A]()
      p.success(a)
//      js.Dynamic.global.setTimeout(() => p.success(a), 0)
      p.future
    }

  val failedPromiseGen: Gen[Future[Nothing]] =
    Gen.delay(Gen.const {
      val p = new Promise[Nothing]()
      p.failure(new Exception("failed"))
//      js.Dynamic.global.setTimeout(() => p.failure("failed"), 0)
      p.future
    })

  implicit def arbitraryFuture[A](implicit arbA: Arbitrary[A]): Arbitrary[Future[A]] =
    Arbitrary(Gen.oneOf(successfulGen[A], successfulPromiseGen[A], failedGen, failedPromiseGen))

}

object EqFuture {

  implicit val eqThrowable: Eq[Throwable] = Eq.fromUniversalEquals

  implicit def eqFuture[A](implicit eqA: Eq[Option[Xor[Throwable, A]]]): Eq[Future[A]] = {

    def completion(fa: Future[A]): Option[Xor[Throwable, A]] = {
      var result: Option[Xor[Throwable, A]] = None
      fa(a => result = Some(Xor.Right(a)), error => result = Some(Xor.Left(error)))
      result
    }
    new Eq[Future[A]] {
      def eqv(fa1: Future[A], fa2: Future[A]) =
        eqA.eqv(completion(fa1), completion(fa2))
    }
  }

}
