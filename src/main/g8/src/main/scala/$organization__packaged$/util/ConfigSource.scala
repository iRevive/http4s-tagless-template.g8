package $organization$.util

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.functor.*
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigReader
import pureconfig.error.ConfigReaderException
import pureconfig.generic.derivation.default.*

import scala.annotation.nowarn
import scala.reflect.ClassTag

class ConfigSource[F[_]: Sync](source: pureconfig.ConfigSource) {

  def get[A: ConfigReader: ClassTag](namespace: String): F[A] =
    EitherT(Sync[F].blocking(source.at(namespace).cursor()))
      .subflatMap(ConfigReader[A].from)
      .leftMap(ConfigReaderException[A])
      .rethrowT

}

object ConfigSource {

  def fromTypesafeConfig[F[_]: Sync]: F[ConfigSource[F]] =
    for {
      config <- Sync[F].blocking(ConfigFactory.load())
    } yield new ConfigSource[F](pureconfig.ConfigSource.fromConfig(config))

}
