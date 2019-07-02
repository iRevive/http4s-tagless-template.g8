package $organization$

import cats.Monad
import cats.effect._
import $organization$.ApplicationLoader.Application
import $organization$.api.{ApiModule, ApiModuleLoader}
import $organization$.persistence.{PersistenceModule, PersistenceModuleLoader}
import $organization$.processing.{ProcessingModule, ProcessingModuleLoader}
import $organization$.util.error.ErrorHandle
import $organization$.util.logging.TraceProvider
import com.typesafe.config.Config

class ApplicationLoader[F[_]: Monad](
    persistenceModuleLoader: PersistenceModuleLoader[F],
    processingModuleLoader: ProcessingModuleLoader[F],
    apiModuleLoader: ApiModuleLoader[F]
) {

  def load(config: Config): Resource[F, Application[F]] =
    for {
      persistenceModule <- persistenceModuleLoader.load(config)
      processingModule  <- processingModuleLoader.load(config, persistenceModule)
      apiModule         <- Resource.liftF(apiModuleLoader.load(config))
    } yield Application(persistenceModule, processingModule, apiModule)

}

object ApplicationLoader {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider]: ApplicationLoader[F] =
    new ApplicationLoader[F](
      PersistenceModuleLoader.default,
      new ProcessingModuleLoader[F],
      new ApiModuleLoader[F]
    )

  final case class Application[F[_]](
      persistenceModule: PersistenceModule[F],
      processingModule: ProcessingModule,
      apiModule: ApiModule[F]
  )

}
