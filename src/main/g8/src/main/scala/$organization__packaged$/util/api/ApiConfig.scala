package $organization$.util.api

import $organization$.util.instances.render.*
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.refined.*
import io.odin.meta.Render
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class ApiConfig(host: String, port: Int, auth: BasicAuthConfig) derives ConfigReader

object ApiConfig {
  given apiConfigRender: Render[ApiConfig] = {
    import io.odin.extras.derivation.render.derived
    Render.derived
  }
}
