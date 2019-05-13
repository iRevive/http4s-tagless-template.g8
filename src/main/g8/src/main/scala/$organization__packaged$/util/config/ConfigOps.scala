package $organization$.util
package config

import cats.effect.Sync
import cats.syntax.flatMap._
import $organization$.util.error.{BaseError, ErrorRaise}
import $organization$.util.syntax.logging._
import cats.syntax.either._
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.{Decoder, Error, ParsingFailure}

import scala.language.implicitConversions
import scala.reflect.ClassTag

final class RichConfig(private val config: Config) extends AnyVal {
  import io.circe.config.syntax._

  def loadF[F[_]: Sync: ErrorRaise, A: Decoder: ClassTag](path: String): F[A] =
    Sync[F].delay(load[A](path)).flatMap(v => v.fold(ErrorRaise[F].raise, Sync[F].pure))

  def loadMetaF[F[_]: Sync: ErrorRaise, A: Decoder: ClassTag](path: String): F[A] =
    Sync[F].delay(loadMeta[A](path)).flatMap(v => v.fold(ErrorRaise[F].raise, Sync[F].pure))

  def load[A: Decoder: ClassTag](path: String): Either[BaseError, A] = {
    config.as[A](path).leftMap(error => ConfigParsingError(path, ClassUtils.classSimpleName, error))
  }

  def loadMeta[A: Decoder: ClassTag](path: String): Either[BaseError, A] = {
    parseStringAsConfig(config.getString(path))
      .flatMap(_.as[A])
      .leftMap(error => ConfigParsingError(path, ClassUtils.classSimpleName, error))
  }

  private def parseStringAsConfig(input: => String): Either[Error, Config] = {
    Either
      .catchNonFatal(ConfigFactory.parseString(input))
      .leftMap(e => ParsingFailure(log"Couldn't parse [\$input] as config", e))
  }

}

trait ToConfigOps {

  final implicit def toRichConfig(config: Config): RichConfig = new RichConfig(config)

}

final case class ConfigParsingError(path: String, expectedClass: String, error: Error)(implicit val pos: Position)
    extends BaseError {

  override def message: String = log"Couldn't load [\$expectedClass] at path [\$path]. Error [\${error.getMessage}]"

}
