package $organization$.util

import cats.mtl.{DefaultApplicativeHandle, DefaultApplicativeLocal}
import cats.{Applicative, Functor}
import $organization$.test.BaseSpec
import $organization$.util.error.{BaseError, ErrorHandle, ThrowableError}
import $organization$.util.logging.{TraceId, TraceProvider}
import eu.timepit.refined.auto._
import monix.eval.Task

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

class ExecutionOpsSpec extends BaseSpec {

  "ExecutionOps" when {

    "retry" should {

      "execute task only once in case of no error" in TaskAssertion {
        var inc = 0

        val executionResult = "test func"

        val task = Task {
          inc = inc + 1
          executionResult
        }

        val timeoutError = Task.raiseError[String](
          new TimeoutException("Timeout exception")
        )

        val retryPolicy = RetryPolicy(5, 10.millis, 100.millis)

        for {
          result <- ExecutionOps.retry("retry spec", task, retryPolicy, timeoutError)
        } yield {
          result shouldBe executionResult
          inc shouldBe 1
        }
      }

      "re-execute task required amount of retries" in {
        var inc = 0

        val task = Task {
          inc = inc + 1
          throw new Exception("It's not working")
        }

        val timeoutException = new TimeoutException("Timeout exception")
        val timeoutError     = Task.raiseError[Unit](timeoutException)

        val retryPolicy = RetryPolicy(5, 30.millis, 1000.millis)

        for {
          result <- ExecutionOps.retry("retry spec", task, retryPolicy, timeoutError).attempt
        } yield {
          result.leftValue shouldBe timeoutException
          inc shouldBe 6
        }
      }

      "use a timeout task as a fallback in case of timeout" in {
        var inc = 0

        val exception = new Exception("It's not working")

        val timeoutException = new TimeoutException("Timeout exception")

        val task = Task {
          inc = inc + 1
          throw exception
        }

        val timeoutError = Task.raiseError[Unit](timeoutException)

        val retryPolicy = RetryPolicy(5, 30.millis, 40.millis)

        for {
          result <- ExecutionOps.retry("retry spec", task, retryPolicy, timeoutError).attempt
        } yield {
          result.leftValue shouldBe timeoutException
          inc shouldBe 2
        }
      }

    }

  }

  private implicit val traceProvider: TraceProvider[Task] = new DefaultApplicativeLocal[Task, TraceId] {
    override def local[A](f: TraceId => TraceId)(fa: Task[A]): Task[A] = fa
    override val applicative: Applicative[Task]                        = implicitly
    override def ask: Task[TraceId]                                    = Task.eval(TraceId.randomUuid())
  }

  private implicit val errorHandle: ErrorHandle[Task] = new DefaultApplicativeHandle[Task, BaseError] {
    override val applicative: Applicative[Task] = implicitly
    override val functor: Functor[Task]         = implicitly
    override def handleWith[A](fa: Task[A])(f: BaseError => Task[A]): Task[A] = fa.onErrorRecoverWith {
      case e => f(ThrowableError(e))
    }
    override def raise[A](e: BaseError): Task[A] = Task.raiseError(e.toRuntimeException)
  }

}
