package $organization$.util.json

import cats.syntax.either._
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.Coercible
import org.http4s.Uri

object JsonCodecs {

  implicit def coercibleDecoder[R, N](implicit ev: Coercible[Decoder[R], Decoder[N]], R: Decoder[R]): Decoder[N] =
    ev(R)

  implicit def coercibleEncoder[R, N](implicit ev: Coercible[Encoder[R], Encoder[N]], R: Encoder[R]): Encoder[N] =
    ev(R)

  implicit val uriDecoder: Decoder[Uri] = Decoder[String].emap(v => Uri.fromString(v).leftMap(_.message))

}
