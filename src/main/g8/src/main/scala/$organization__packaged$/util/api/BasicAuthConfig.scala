package $organization$.util.api

import io.circe.{Decoder, Encoder}
import io.odin.meta.Render

@scalaz.deriving(Decoder, Encoder, Render)
final case class BasicAuthConfig(
    realm: String,
    user: String,
    password: String
)
