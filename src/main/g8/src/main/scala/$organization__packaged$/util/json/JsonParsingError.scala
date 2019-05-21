package $organization$.util
package json

import java.nio.charset.Charset

import cats.data.NonEmptyList
import $organization$.util.logging.Loggable
import $organization$.util.syntax.logging._
import io.circe.{DecodingFailure, Json, ParsingFailure}

@scalaz.deriving(Loggable)
sealed trait JsonParsingError

@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
object JsonParsingError {

  final case class UnsupportedString(input: Array[Byte], charset: Charset, cause: Throwable) extends JsonParsingError

  final case class NonParsableJson(input: String, failure: ParsingFailure) extends JsonParsingError

  final case class JsonDecodingError(json: Json, className: String, errors: NonEmptyList[DecodingFailure])
      extends JsonParsingError

  implicit val byteArrayLoggable: Loggable[Array[Byte]] = v => log"Array length [\${v.length}]"
  implicit val charsetLoggable: Loggable[Charset]       = v => v.name()

}
