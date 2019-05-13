package $organization$.util

import cats.mtl.implicits._
import cats.syntax.applicativeError._
import $organization$.test.BaseSpec
import eu.timepit.refined.auto._

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

class ExecutionOpsSpec extends BaseSpec {

  "ExecutionOps" when {

    "retry" should {

      "execute task only once in case of no error" in EffectAssertion() {
        var inc = 0

        val executionResult = "test func"

        val fa = Eff.delay {
          inc = inc + 1
          executionResult
        }

        val timeoutError = Eff.raiseError[String](
          new TimeoutException("Timeout exception")
        )

        val retryPolicy = RetryPolicy(5, 10.millis, 100.millis)

        for {
          result <- ExecutionOps.retry("retry spec", fa, retryPolicy, timeoutError)
        } yield {
          result shouldBe executionResult
          inc shouldBe 1
        }
      }

      "re-execute task required amount of retries" in EffectAssertion() {
        var inc = 0

        val fa = Eff.delay[Unit] {
          inc = inc + 1
          throw new Exception("It's not working")
        }

        val timeoutException = new TimeoutException("Timeout exception")
        val timeoutError     = Eff.raiseError[Unit](timeoutException)

        val retryPolicy = RetryPolicy(5, 30.millis, 1000.millis)

        for {
          result <- ExecutionOps.retry("retry spec", fa, retryPolicy, timeoutError).attempt
        } yield {
          result.leftValue shouldBe timeoutException
          inc shouldBe 6
        }
      }

      "use a timeout task as a fallback in case of timeout" in EffectAssertion() {
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
          result.leftValue shouldBe timeoutException
          inc shouldBe 2
        }
      }

    }

  }

}
