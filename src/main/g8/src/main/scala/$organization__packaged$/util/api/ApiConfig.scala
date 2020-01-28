package $organization$.util.api

import $organization$.util.instances.render._
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.refined._
import io.odin.meta.Render

@scalaz.deriving(Decoder, Render)
final case class ApiConfig(host: NonEmptyString, port: PosInt, auth: BasicAuthConfig)
