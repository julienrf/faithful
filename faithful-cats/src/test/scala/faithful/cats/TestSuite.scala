package faithful.cats

import cats.{ApplicativeError, Eq}
import cats.data.EitherT
import cats.data.EitherT.catsDataEqForEitherT
import faithful.{Future, Promise}
import cats.laws.discipline.{ApplicativeErrorTests, ApplicativeTests, MonadErrorTests}
import cats.laws.discipline.CartesianTests.Isomorphisms
import cats.instances.int._
import cats.instances.option.catsKernelStdEqForOption
import cats.instances.either.catsStdEqForEither
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

// TODO Write tests that don’t need `Eq[Future[_]]` values
class TestSuite extends FunSuite with Discipline {

  import faithful.cats.Instances._
  import ArbitraryFuture._
  import EqFuture._

  implicit val eqEitherTFEA: Eq[EitherT[Future, Throwable, Int]] = catsDataEqForEitherT[Future, Throwable, Int]
  implicit val eqIntIntInt: Eq[(Int,Int,Int)] =
    (l: (Int, Int, Int), r: (Int, Int, Int)) =>
      Eq[Int].eqv(l._1, r._1) &&
        Eq[Int].eqv(l._2, r._2) &&
        Eq[Int].eqv(l._3, r._3)
  implicit val iso: Isomorphisms[Future] = Isomorphisms.invariant[Future]

  checkAll("Future[Int]", ApplicativeTests[Future].applicative[Int, Int, Int])
//  checkAll("Future[Int]", ApplicativeErrorTests[Future, Throwable].applicativeError[Int, Int, Int])
//  checkAll("Future[Int]", MonadErrorTests[Future, Throwable].stackUnsafeMonad[Int, Int, Int])

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

  implicit def eqFuture[A](implicit eqA: Eq[Option[Either[Throwable, A]]]): Eq[Future[A]] =
    (fa1: Future[A], fa2: Future[A]) => eqA.eqv(Future.completion(fa1), Future.completion(fa2))

}
