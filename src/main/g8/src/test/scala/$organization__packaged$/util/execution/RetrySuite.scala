package $organization$.util.execution

import cats.data.EitherT
import cats.data.EitherT.catsDataMonadErrorFForEitherT
import cats.effect.{Async, IO, Ref}
import cats.mtl.Handle.{handleEitherT, handleKleisli}
import cats.mtl.implicits.*
import cats.syntax.applicativeError.*
import cats.syntax.apply.*
import cats.syntax.functor.*
import $organization$.util.execution.RetrySuite.*
import eu.timepit.refined.auto.*
import io.odin.extras.derivation.render.derived
import io.odin.meta.Render
import retry.mtl.syntax.all.*
import retry.syntax.all.*
import weaver.SimpleIOSuite

import scala.concurrent.duration.*

object RetrySuite extends SimpleIOSuite {

  private type Eff[A] = EitherT[IO, ExecutionError, A]
  private val Eff = Async[Eff]

  test("execute task only once in case of no error") {
    val executionResult = "test func"

    def fa(counter: Ref[Eff, Int]) =
      counter.update(_ + 1).as(executionResult)

    val retryPolicy = Retry.Policy(5, 10.millis, 100.millis)

    for {
      counter <- Ref.of(0)
      result <- fa(counter.mapK(EitherT.liftK))
        .retryingOnAllMtlErrors[ExecutionError](Retry.makePolicy(retryPolicy), retry.noop)
        .value
      attempts <- counter.get
    } yield expect.all(result == Right(executionResult), attempts == 1)
  }

  test("re-execute task required amount of retries in case of error") {
    val executionError = ExecutionError("error 1")

    def fa(counter: Ref[Eff, Int]) =
      counter.update(_ + 1) *> executionError.raise[Eff, Unit]

    val retryPolicy = Retry.Policy(5, 30.millis, 1000.millis)

    for {
      counter <- Ref.of(0)
      result <- fa(counter.mapK(EitherT.liftK))
        .retryingOnAllMtlErrors[ExecutionError](Retry.makePolicy(retryPolicy), retry.noop)
        .attemptHandle[ExecutionError]
        .value
      attempts <- counter.get
    } yield expect.all(result == Right(Left(executionError)), attempts == 6)
  }

  test("re-execute task required amount of retries in case of unhandled exception") {
    val exception = new Exception("It's not working")

    def fa(counter: Ref[Eff, Int]) =
      counter.update(_ + 1) *> Eff.delay[Unit](throw exception)

    val retryPolicy = Retry.Policy(5, 30.millis, 400.millis)

    for {
      counter  <- Ref.of(0)
      result   <- fa(counter.mapK(EitherT.liftK)).retryingOnAllErrors(Retry.makePolicy(retryPolicy), retry.noop).attempt.value
      attempts <- counter.get
    } yield expect.all(result == Right(Left(exception)), attempts == 6)
  }

  test("give up when timeout reached") {
    val exception = new Exception("It's not working")

    def fa(counter: Ref[Eff, Int]) =
      counter.update(_ + 1) *> Eff.delay[Unit](throw exception)

    val retryPolicy = Retry.Policy(5, 30.millis, 40.millis)

    for {
      counter  <- Ref.of(0)
      result   <- fa(counter.mapK(EitherT.liftK)).retryingOnAllErrors(Retry.makePolicy(retryPolicy), retry.noop).attempt.value
      attempts <- counter.get
    } yield expect.all(result == Right(Left(exception)), attempts == 2)
  }

  final case class ExecutionError(cause: String) derives Render

}
