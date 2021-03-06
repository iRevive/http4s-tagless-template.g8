package $organization$.util.execution

import cats.effect.concurrent.Ref
import cats.mtl.implicits._
import cats.mtl.Handle.handleKleisli
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.functor._
import $organization$.test.EffectSpec
import $organization$.util.execution.RetrySpec._
import eu.timepit.refined.auto._
import io.odin.meta.Render
import retry.syntax.all._
import retry.mtl.syntax.all._

import scala.concurrent.duration._

class RetrySpec extends EffectSpec[ExecutionError] {

  "Retry" should {

    "execute task only once in case of no error" in EffectAssertion() {
      val executionResult = "test func"

      def fa(counter: Ref[Eff, Int]) =
        counter.update(_ + 1).as(executionResult)

      val retryPolicy = Retry.Policy(5, 10.millis, 100.millis)

      for {
        counter  <- Ref.of(0)
        result   <- fa(counter).retryingOnAllMtlErrors[ExecutionError](Retry.makePolicy(retryPolicy), retry.noop)
        attempts <- counter.get
      } yield {
        result shouldBe executionResult
        attempts shouldBe 1
      }
    }

    "re-execute task required amount of retries in case of error" in EffectAssertion() {
      val exception1 = ExecutionError("error 1")

      def fa(counter: Ref[Eff, Int]) =
        counter.update(_ + 1) *> exception1.raise[Eff, Unit]

      val retryPolicy = Retry.Policy(5, 30.millis, 1000.millis)

      for {
        counter <- Ref.of(0)
        result <- fa(counter)
          .retryingOnAllMtlErrors[ExecutionError](Retry.makePolicy(retryPolicy), retry.noop)
          .attemptHandle[ExecutionError]
        attempts <- counter.get
      } yield {
        result.leftValue shouldBe exception1
        attempts shouldBe 6
      }
    }

    "re-execute task required amount of retries in case of unhandled exception" in EffectAssertion() {
      val exception = new Exception("It's not working")

      def fa(counter: Ref[Eff, Int]) =
        counter.update(_ + 1) *> Eff.delay[Unit](throw exception)

      val retryPolicy = Retry.Policy(5, 30.millis, 400.millis)

      for {
        counter  <- Ref.of(0)
        result   <- fa(counter).retryingOnAllErrors(Retry.makePolicy(retryPolicy), retry.noop).attempt
        attempts <- counter.get
      } yield {
        result.leftValue shouldBe exception
        attempts shouldBe 6
      }
    }

    "give up when timeout reached" in EffectAssertion() {
      val exception = new Exception("It's not working")

      def fa(counter: Ref[Eff, Int]) =
        counter.update(_ + 1) *> Eff.delay[Unit](throw exception)

      val retryPolicy = Retry.Policy(5, 30.millis, 40.millis)

      for {
        counter  <- Ref.of(0)
        result   <- fa(counter).retryingOnAllErrors(Retry.makePolicy(retryPolicy), retry.noop).attempt
        attempts <- counter.get
      } yield {
        result.leftValue shouldBe exception
        attempts shouldBe 2
      }
    }

  }

}

object RetrySpec {

  @scalaz.annotation.deriving(Render)
  final case class ExecutionError(cause: String)

}
