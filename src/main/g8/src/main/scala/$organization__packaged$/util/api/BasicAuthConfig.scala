package $organization$.util.api

import io.odin.meta.Render
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class BasicAuthConfig(
    realm: String,
    user: String,
    password: String
) derives ConfigReader

object BaseAuthConfig {
  given baseAuthConfigRender: Render[BasicAuthConfig] = {
    import io.odin.extras.derivation.render.derived
    Render.derived
  }
}
