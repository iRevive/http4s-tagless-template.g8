package $organization$.persistence.mongo

import $organization$.util.execution.Retry
import $organization$.util.logging.Loggable
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.FiniteDuration

@scalaz.deriving(Loggable)
final case class MongoConfig(
    uri: String Refined Uri,
    database: NonEmptyString,
    connectionAttemptTimeout: FiniteDuration,
    retryPolicy: Retry.Policy
)

object MongoConfig {

  import io.circe.Decoder
  import io.circe.config.syntax.durationDecoder
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.auto._
  import io.circe.refined._

  implicit val configuration: Configuration  = Configuration.default.withKebabCaseMemberNames
  implicit val decoder: Decoder[MongoConfig] = exportDecoder[MongoConfig].instance

}
