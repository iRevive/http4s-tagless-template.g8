package $organization$.util

import cats.Monad
import cats.effect.{Concurrent, Timer}
import cats.syntax.flatMap._
import cats.syntax.apply._
import cats.mtl.syntax.handle._
import $organization$.util.error.{ErrorHandle, RaisedError}
import $organization$.util.logging.{TraceProvider, TracedLogger}
import $organization$.util.syntax.logging._
import eu.timepit.refined.types.numeric.NonNegInt

import scala.concurrent.duration.FiniteDuration

object ExecutionOps {

  def delayExecution[F[_]: Monad: Timer, A](flow: F[A], timespan: FiniteDuration): F[A] =
    Timer[F].sleep(timespan) >> flow

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def retry[F[_]: Concurrent: Timer: TraceProvider: ErrorHandle, A](
      name: String,
      fa: F[A],
      retryPolicy: RetryPolicy,
      onTimeout: F[A]
  ): F[A] = {
    val logger = TracedLogger.create[F](getClass)

    val result = fa.handleWith[RaisedError] {
      case error if retryPolicy.retries.value > 0 =>
        val newPolicy = retryPolicy.copy(retries = NonNegInt.unsafeFrom(retryPolicy.retries.value - 1))

        logger.error(log"[\$name] Retry policy. Current policy \$retryPolicy. Error \$error") *>
          delayExecution(retry(name, fa, newPolicy, onTimeout), retryPolicy.delay)

      case error =>
        logger.error(log"[\$name] Retry policy. Zero retries left. Error \$error", error) >> onTimeout
    }

    Concurrent.timeoutTo[F, A](result, retryPolicy.timeout, onTimeout)
  }

}
