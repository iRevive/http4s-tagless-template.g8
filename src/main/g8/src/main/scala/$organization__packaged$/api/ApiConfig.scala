package $organization$.api

import $organization$.util.logging.{Loggable, LoggableDerivation}
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

final case class ApiConfig(host: NonEmptyString, port: PosInt)

object ApiConfig {

  import io.circe.Decoder
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.auto._
  import io.circe.refined._

  implicit val configuration: Configuration          = Configuration.default.withKebabCaseMemberNames
  implicit val decoder: Decoder[ApiConfig]           = exportDecoder[ApiConfig].instance
  implicit val loggableInstance: Loggable[ApiConfig] = LoggableDerivation.derive

}
