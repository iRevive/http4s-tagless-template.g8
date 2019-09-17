package $organization$.util.execution

import cats.effect.concurrent.Ref
import cats.mtl.implicits._
import cats.mtl.ApplicativeHandle
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.functor._
import $organization$.test.EffectSpec
import $organization$.util.execution.RetrySpec._
import $organization$.util.logging.Loggable
import $organization$.util.syntax.retry._
import eu.timepit.refined.auto._

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
        result   <- fa(counter).retry[ExecutionError](retryPolicy, Retry.Decider.default, Retry.Logger.noop)
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
        result <- ApplicativeHandle[Eff, ExecutionError].attempt(
          fa(counter).retry[ExecutionError](retryPolicy, Retry.Decider.default, Retry.Logger.noop)
        )
        attempts <- counter.get
      } yield {
        result.leftValue shouldBe exception1
        attempts shouldBe 5
      }
    }

    "re-execute task required amount of retries in case of unhandled exception" in EffectAssertion() {
      val exception = new Exception("It's not working")

      def fa(counter: Ref[Eff, Int]) =
        counter.update(_ + 1) *> Eff.delay[Unit](throw exception)

      val retryPolicy = Retry.Policy(5, 30.millis, 40.millis)

      for {
        counter  <- Ref.of(0)
        result   <- fa(counter).retry[ExecutionError](retryPolicy, Retry.Decider.default, Retry.Logger.noop).attempt
        attempts <- counter.get
      } yield {
        result.leftValue shouldBe exception
        attempts shouldBe 5
      }
    }

  }

}

object RetrySpec {

  @scalaz.deriving(Loggable)
  final case class ExecutionError(cause: String)

}
