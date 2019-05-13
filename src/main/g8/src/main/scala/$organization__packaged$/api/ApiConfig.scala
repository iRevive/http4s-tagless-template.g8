package $organization$.api

import $organization$.util.logging.Loggable
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.refined._

@scalaz.deriving(Decoder, Loggable)
final case class ApiConfig(host: NonEmptyString, port: PosInt)
