package faithful.cats

import cats.{ContravariantCartesian, Eq, MonadError}
import cats.data.EitherT
import faithful.{Future, Promise}
import cats.laws.discipline.{ApplicativeErrorTests, FlatMapTests, catsLawsIsEqToProp}
import cats.laws.discipline.CartesianTests.Isomorphisms
import cats.instances.int._
import cats.instances.unit._
import cats.instances.option.catsKernelStdEqForOption
import cats.instances.either.catsStdEqForEither
import cats.laws.{ApplicativeErrorLaws, FlatMapLaws, IsEq}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

// TODO Write tests that don’t need `Eq[Future[_]]` values
class TestSuite extends FunSuite with Discipline {

  import faithful.cats.Instances._
  import ArbitraryFuture._
  import EqFuture._

  implicit val eqIntIntInt: Eq[(Int,Int,Int)] =
    (l: (Int, Int, Int), r: (Int, Int, Int)) =>
      Eq[Int].eqv(l._1, r._1) &&
        Eq[Int].eqv(l._2, r._2) &&
        Eq[Int].eqv(l._3, r._3)

  implicit val iso: Isomorphisms[Future] = Isomorphisms.invariant[Future]

  //  implicit val eqEitherTFEA: Eq[EitherT[Future, Throwable, Int]] = catsDataEqForEitherT[Future, Throwable, Int]
//  checkAll("Future[Int]", ApplicativeTests[Future].applicative[Int, Int, Int])
//  checkAll("Future[Int]", ApplicativeErrorTests[Future, Throwable].applicativeError[Int, Int, Int])
//  checkAll("Future[Int]", FlatMapTests[Future].flatMap[Int, Int, Int])
  checkAll("Future[Int]", FutureTests.future[Int, Int, Int])

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

// We don’t reuse cats’ tests because the assume a stack-safe `tailRecM` implementation
object FutureTests extends ApplicativeErrorTests[Future, Throwable] with FlatMapTests[Future] {

  import faithful.cats.Instances._
  import cats.laws.discipline.arbitrary.catsLawsArbitraryForPartialFunction

  object laws extends ApplicativeErrorLaws[Future, Throwable] with FlatMapLaws[Future] {
    import cats.syntax.all._
    import cats.laws.IsEqArrow

    val F: MonadError[Future, Throwable] = implicitly

    // MonadError laws
    def monadErrorLeftZero[A, B](e: Throwable, f: A => Future[B]): IsEq[Future[B]] =
      F.flatMap(F.raiseError[A](e))(f) <-> F.raiseError[B](e)

    // Monad laws
    def monadLeftIdentity[A, B](a: A, f: A => Future[B]): IsEq[Future[B]] =
      F.pure(a).flatMap(f) <-> f(a)

    def monadRightIdentity[A](fa: Future[A]): IsEq[Future[A]] =
      fa.flatMap(F.pure) <-> fa

    def mapFlatMapCoherence[A, B](fa: Future[A], f: A => B): IsEq[Future[B]] =
      fa.flatMap(a => F.pure(f(a))) <-> fa.map(f)

  }

  def future[A : Arbitrary : Eq, B : Arbitrary : Eq, C : Arbitrary : Eq](implicit
    ArbFA: Arbitrary[Future[A]],
    ArbFB: Arbitrary[Future[B]],
    ArbFC: Arbitrary[Future[C]],
    ArbFAtoB: Arbitrary[Future[A => B]],
    ArbFBtoC: Arbitrary[Future[B => C]],
    ArbE: Arbitrary[Throwable],
    CogenA: Cogen[A],
    CogenB: Cogen[B],
    CogenC: Cogen[C],
    CogenE: Cogen[Throwable],
    EqFA: Eq[Future[A]],
    EqFB: Eq[Future[B]],
    EqFC: Eq[Future[C]],
    EqE: Eq[Throwable],
    EqFEitherEU: Eq[Future[Either[Throwable, Unit]]],
    EqFEitherEA: Eq[Future[Either[Throwable, A]]],
    EqEitherTFEA: Eq[EitherT[Future, Throwable, A]],
    EqFABC: Eq[Future[(A, B, C)]],
    EqFInt: Eq[Future[Int]],
    iso: Isomorphisms[Future]
  ): RuleSet = {
    implicit val EqFAB: Eq[Future[(A, B)]] =
      ContravariantCartesian[Eq].composeFunctor[Future].product(EqFA, EqFB)

    new RuleSet {
      val name = "future"
      val parents = Seq(applicative[A, B, C])
      val bases = Nil
      val props = Seq(
        // MonadError rules
        "monadError left zero" -> forAll(laws.monadErrorLeftZero[A, B] _),
        // Monad rules (excepted “tailRecM stack safety”)
        "monad left identity" -> forAll(laws.monadLeftIdentity[A, B] _),
        "monad right identity" -> forAll(laws.monadRightIdentity[A] _),
        "map flatMap coherence" -> forAll(laws.mapFlatMapCoherence[A, B] _),
        // FlatMap rules
//        "flatMap associativity" -> forAll(laws.flatMapAssociativity[A, B, C] _),
        "flatMap consistent apply" -> forAll(laws.flatMapConsistentApply[A, B] _),
        "followedBy consistent flatMap" -> forAll(laws.followedByConsistency[A, B] _),
//        "mproduct consistent flatMap" -> forAll(laws.mproductConsistency[A, B] _),
        "tailRecM consistent flatMap" -> forAll(laws.tailRecMConsistentFlatMap[A] _),
        // ApplicativeError rules
        "applicativeError handleWith" -> forAll(laws.applicativeErrorHandleWith[A] _),
        "applicativeError handle" -> forAll(laws.applicativeErrorHandle[A] _),
        "applicativeError handleErrorWith pure" -> forAll(laws.handleErrorWithPure[A] _),
        "applicativeError handleError pure" -> forAll(laws.handleErrorPure[A] _),
        "applicativeError raiseError attempt" -> forAll(laws.raiseErrorAttempt _),
        "applicativeError pure attempt" -> forAll(laws.pureAttempt[A] _),
//        "applicativeError handleErrorWith consistent with recoverWith" -> forAll(laws.handleErrorWithConsistentWithRecoverWith[A] _),
//        "applicativeError handleError consistent with recover" -> forAll(laws.handleErrorConsistentWithRecover[A] _),
//        "applicativeError recover consistent with recoverWith" -> forAll(laws.recoverConsistentWithRecoverWith[A] _),
        "applicativeError attempt consistent with attemptT" -> forAll(laws.attemptConsistentWithAttemptT[A] _)
      )
    }
  }

}
