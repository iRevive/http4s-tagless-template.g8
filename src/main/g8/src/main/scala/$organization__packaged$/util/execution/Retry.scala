package $organization$.util.execution

import cats.Applicative
import $organization$.util.error.ThrowableSelect
import $organization$.util.logging.{Loggable, TracedLogger}
import $organization$.util.logging.LoggableDerivation._
import $organization$.util.syntax.logging._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.Decoder
import io.circe.config.syntax.durationDecoder
import io.circe.refined._
import retry.{RetryDetails, RetryPolicies, RetryPolicy}

import scala.concurrent.duration.FiniteDuration

object Retry {

  def makePolicy[F[_]: Applicative](retryPolicy: Policy): RetryPolicy[F] = {
    val policy = RetryPolicies
      .constantDelay(retryPolicy.delay)
      .join(RetryPolicies.limitRetries(retryPolicy.retries))

    RetryPolicies.limitRetriesByCumulativeDelay(retryPolicy.timeout, policy)
  }

  def logErrors[F[_]: Applicative, E: Loggable: ThrowableSelect](logger: TracedLogger[F]): (E, RetryDetails) => F[Unit] =
    (error, details) => logger.error(log"Retry policy. Error \$error. \$details", error)

  @scalaz.deriving(Decoder, Loggable)
  final case class Policy(retries: NonNegInt, delay: FiniteDuration, timeout: FiniteDuration)

}
