package $organization$.util

import $organization$.util.logging.{Loggable, LoggableDerivation}
import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto.exportDecoder

import scala.concurrent.duration.FiniteDuration

final case class RetryPolicy(retries: NonNegInt, delay: FiniteDuration, timeout: FiniteDuration)

object RetryPolicy {

  import io.circe.config.syntax.durationDecoder
  import io.circe.refined._

  implicit val configuration: Configuration               = Configuration.default.withKebabCaseMemberNames
  implicit val retryPolicyDecoder: Decoder[RetryPolicy]   = exportDecoder[RetryPolicy].instance
  implicit val retryPolicyLoggable: Loggable[RetryPolicy] = LoggableDerivation.derive

}
