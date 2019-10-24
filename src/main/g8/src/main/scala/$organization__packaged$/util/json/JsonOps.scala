package $organization$.util
package json

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.flatMap._
import $organization$.util.error.{EmptyThrowableExtractor, ErrorIdGen, ErrorRaise}
import $organization$.util.logging.Loggable
import $organization$.util.syntax.mtl.raise._
import io.circe._

import scala.reflect.ClassTag

trait ToJsonOps {
  final implicit def toJsonOps(json: Json): JsonOps = new JsonOps(json)
}

final class JsonOps(private val json: Json) extends AnyVal {

  def decodeF[F[_]: Sync: ErrorRaise: ErrorIdGen, A: ClassTag: Decoder]: F[A] =
    Sync[F].delay(decode[A]).flatMap(_.pureOrRaise)

  def decode[A](implicit decoder: Decoder[A], ct: ClassTag[A]): Either[JsonDecodingError, A] =
    decoder
      .decodeAccumulating(json.hcursor)
      .toEither
      .leftMap(errors => JsonDecodingError(json, ct.runtimeClass.getSimpleName, errors))

}

@scalaz.deriving(Loggable, EmptyThrowableExtractor)
final case class JsonDecodingError(
    json: Json,
    targetClass: String,
    errors: NonEmptyList[DecodingFailure]
)
