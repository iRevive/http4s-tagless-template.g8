package $organization$.persistence.postgres

import $organization$.util.Retry
import $organization$.util.logging.Loggable
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.config.syntax.durationDecoder
import io.circe.refined._

import scala.concurrent.duration.FiniteDuration

@scalaz.deriving(Loggable)
final case class PostgresConfig(
    driver: NonEmptyString,
    uri: NonEmptyString,
    user: NonEmptyString,
    password: String,
    connectionAttemptTimeout: FiniteDuration,
    retryPolicy: Retry.Policy
)

object PostgresConfig {

  implicit val configuration: Configuration     = Configuration.default.withKebabCaseMemberNames
  implicit val decoder: Decoder[PostgresConfig] = deriveDecoder

}
