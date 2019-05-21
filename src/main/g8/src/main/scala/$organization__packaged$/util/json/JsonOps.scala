package $organization$.util
package json

import java.nio.charset.{Charset, StandardCharsets}

import cats.syntax.either._
import io.circe._
import io.circe.parser._

import scala.reflect.ClassTag

@SuppressWarnings(Array("org.wartremover.warts.Overloading", "org.wartremover.warts.DefaultArguments"))
object JsonOps {

  import JsonParsingError._

  def parseJson(input: String): Either[JsonParsingError, Json] =
    parse(input).leftMap(e => JsonParsingError.NonParsableJson(input, e))

  def decode[A: Decoder: ClassTag](input: Array[Byte], charset: Charset = StandardCharsets.UTF_8): Either[JsonParsingError, A] =
    for {
      rawJson <- Either.catchNonFatal(new String(input, charset)).leftMap(UnsupportedString(input, charset, _))
      result  <- decode(rawJson)
    } yield result

  def decode[A: Decoder: ClassTag](input: String): Either[JsonParsingError, A] =
    for {
      json   <- JsonOps.parseJson(input)
      result <- JsonOps.asNel[A](json)
    } yield result

  def asNel[A](json: Json)(implicit decoder: Decoder[A], ct: ClassTag[A]): Either[JsonParsingError, A] =
    decoder
      .accumulating(json.hcursor)
      .toEither
      .leftMap(errors => JsonDecodingError(json, ClassUtils.classSimpleName[A], errors))

}
