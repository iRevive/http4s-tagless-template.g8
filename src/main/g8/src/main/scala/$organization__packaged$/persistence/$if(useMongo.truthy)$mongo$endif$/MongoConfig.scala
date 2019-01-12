package $organization$.persistence.mongo

import $organization$.util.RetryPolicy
import $organization$.util.logging.{Loggable, LoggableDerivation}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.FiniteDuration

final case class MongoConfig(
    uri: String Refined Uri,
    database: NonEmptyString,
    connectionAttemptTimeout: FiniteDuration,
    retryPolicy: RetryPolicy
)

object MongoConfig {

  import io.circe.Decoder
  import io.circe.config.syntax.durationDecoder
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.auto._
  import io.circe.refined._

  implicit val configuration: Configuration            = Configuration.default.withKebabCaseMemberNames
  implicit val decoder: Decoder[MongoConfig]           = exportDecoder[MongoConfig].instance
  implicit val loggableInstance: Loggable[MongoConfig] = LoggableDerivation.derive

}
