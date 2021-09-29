package $organization$.util.execution

import cats.Applicative
import $organization$.util.error.ThrowableSelect
import $organization$.util.instances.render.*
import eu.timepit.refined.auto.*
import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.Decoder
import io.circe.refined.*
import io.odin.Logger
import io.odin.meta.Render
import io.odin.syntax.*
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
    (error, details) =>
      ThrowableSelect[E].select(error) match {
        case Some(cause) => logger.error(render"Retry policy. Error \$error. \$details", cause)
        case None        => logger.error(render"Retry policy. Error \$error. \$details")
      }

  final case class Policy(retries: Int, delay: FiniteDuration, timeout: FiniteDuration)

  object Policy {
    given policyRender: Render[Policy] = {
      import io.odin.extras.derivation.render.derived
      Render.derived
    }
  }

  given retryDetailsRender: Render[RetryDetails] = {
    import io.odin.extras.derivation.render.derived
    Render.derived
  }

}
