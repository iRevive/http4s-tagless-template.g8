package $organization$.util
package json

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.either.*
import cats.syntax.flatMap.*
import $organization$.util.error.{ErrorChannel, ThrowableSelect}
import io.circe.*
import io.odin.extras.derivation.render.derived
import io.odin.meta.Render

import scala.reflect.ClassTag

trait ToJsonOps {
  final implicit def toJsonOps(json: Json): JsonOps = new JsonOps(json)
}

final class JsonOps(private val json: Json) extends AnyVal {

  def decodeF[F[_]: Sync: ErrorChannel, A: ClassTag: Decoder]: F[A] =
    ErrorChannel[F].raiseEither(decode[A])

  def decode[A](using decoder: Decoder[A], ct: ClassTag[A]): Either[JsonDecodingError, A] =
    decoder
      .decodeAccumulating(json.hcursor)
      .toEither
      .leftMap(errors => JsonDecodingError(json, ct.runtimeClass.getSimpleName, errors))

}

final case class JsonDecodingError(
    json: Json,
    targetClass: String,
    errors: NonEmptyList[DecodingFailure]
) derives Render, ThrowableSelect.Empty
