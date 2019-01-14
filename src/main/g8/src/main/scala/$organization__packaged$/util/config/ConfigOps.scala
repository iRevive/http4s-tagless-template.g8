package $organization$.util
package config

import $organization$.util.error.BaseError
import $organization$.util.logging.Loggable.InterpolatorOps._
import cats.syntax.either._
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.{Decoder, Error, ParsingFailure}

import scala.language.implicitConversions
import scala.reflect.ClassTag

final class RichConfig(private val config: Config) extends AnyVal {
  import io.circe.config.syntax._

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
