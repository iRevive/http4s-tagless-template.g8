package $organization$.util

import cats.effect.concurrent.Ref
import cats.mtl.implicits._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.functor._
import $organization$.persistence.postgres.PostgresError
import $organization$.test.BaseSpec
import $organization$.util.error.{ErrorHandle, RaisedError}
import eu.timepit.refined.auto._

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

class ExecutionOpsSpec extends BaseSpec {

  "ExecutionOps" when {

    "retry" should {

      "execute task only once in case of no error" in EffectAssertion() {
        val executionResult = "test func"

        def fa(counter: Ref[Eff, Int]) =
          counter.update(_ + 1).as(executionResult)

        val timeoutError = ErrorHandle[Eff].raise[String](
          RaisedError.postgres(PostgresError.ConnectionAttemptTimeout("error 1"))
        )

        val retryPolicy = RetryPolicy(5, 10.millis, 100.millis)

        for {
          counter  <- Ref.of(0)
          result   <- ErrorHandle[Eff].attempt(ExecutionOps.retry("retry spec", fa(counter), retryPolicy, timeoutError))
          attempts <- counter.get
        } yield {
          result shouldBe Right(executionResult)
          attempts shouldBe 1
        }
      }

      "re-execute task required amount of retries" in EffectAssertion() {
        val exception1 = RaisedError.postgres(PostgresError.ConnectionAttemptTimeout("error 1"))
        val exception2 = RaisedError.postgres(PostgresError.ConnectionAttemptTimeout("error 2"))

        def fa(counter: Ref[Eff, Int]) =
          counter.update(_ + 1) *> ErrorHandle[Eff].raise[Unit](exception1)

        val timeoutError = ErrorHandle[Eff].raise[Unit](exception2)

        val retryPolicy = RetryPolicy(5, 30.millis, 1000.millis)

        for {
          counter  <- Ref.of(0)
          result   <- ErrorHandle[Eff].attempt(ExecutionOps.retry("retry spec", fa(counter), retryPolicy, timeoutError))
          attempts <- counter.get
        } yield {
          result.leftValue shouldBe exception2
          attempts shouldBe 6
        }
      }

      "use a timeout task as a fallback in case of timeout" in EffectAssertion() {
        val exception1 = RaisedError.postgres(PostgresError.ConnectionAttemptTimeout("error 1"))
        val exception2 = RaisedError.postgres(PostgresError.ConnectionAttemptTimeout("error 2"))

        def fa(counter: Ref[Eff, Int]) =
          counter.update(_ + 1) *> ErrorHandle[Eff].raise[Unit](exception1)

        val timeoutError = ErrorHandle[Eff].raise[Unit](exception2)

        val retryPolicy = RetryPolicy(5, 30.millis, 40.millis)

        for {
          counter  <- Ref.of(0)
          result   <- ErrorHandle[Eff].attempt(ExecutionOps.retry("retry spec", fa(counter), retryPolicy, timeoutError))
          attempts <- counter.get
        } yield {
          result.leftValue shouldBe exception2
          attempts shouldBe 2
        }
      }

      "rethrow error in case of unhandled exception" in EffectAssertion() {
        var inc = 0

        val exception = new Exception("It's not working")

        val timeoutException = new TimeoutException("Timeout exception")

        val fa = Eff.delay[Unit] {
          inc = inc + 1
          throw exception
        }

        val timeoutError = Eff.raiseError[Unit](timeoutException)

        val retryPolicy = RetryPolicy(5, 30.millis, 40.millis)

        for {
          result <- ExecutionOps.retry("retry spec", fa, retryPolicy, timeoutError).attempt
        } yield {
          result.leftValue shouldBe exception
          inc shouldBe 1
        }
      }

    }

  }

}
