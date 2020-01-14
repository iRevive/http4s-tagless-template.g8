package $organization$.util.execution

import cats.Applicative
import $organization$.util.error.ThrowableSelect
import $organization$.util.logging.RenderInstances._
import $organization$.util.syntax.logging._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.Decoder
import io.circe.config.syntax.durationDecoder
import io.circe.refined._
import io.odin.Logger
import io.odin.meta.Render
import io.odin.syntax._
import retry.{RetryDetails, RetryPolicies, RetryPolicy}

import scala.concurrent.duration.FiniteDuration

object Retry {

  def makePolicy[F[_]: Applicative](retryPolicy: Policy): RetryPolicy[F] = {
    val policy = RetryPolicies
      .constantDelay(retryPolicy.delay)
      .join(RetryPolicies.limitRetries(retryPolicy.retries))

    RetryPolicies.limitRetriesByCumulativeDelay(retryPolicy.timeout, policy)
  }

  def logErrors[F[_]: Applicative, E: Render: ThrowableSelect](logger: Logger[F]): (E, RetryDetails) => F[Unit] =
    (error, details) => logger.error(render"Retry policy. Error \$error. \$details", error)

  @scalaz.deriving(Decoder, Render)
  final case class Policy(retries: NonNegInt, delay: FiniteDuration, timeout: FiniteDuration)

  implicit val renderRetryDetails: Render[RetryDetails] = io.odin.extras.derivation.render.derive

}
