package $organization$.util.api

import $organization$.util.logging.Loggable
import io.circe.{Decoder, Encoder}

@scalaz.deriving(Decoder, Encoder, Loggable)
final case class BasicAuthConfig(
    realm: String,
    user: String,
    password: String
)
