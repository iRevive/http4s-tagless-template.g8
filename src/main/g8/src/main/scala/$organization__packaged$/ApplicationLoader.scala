package $organization$

import cats.effect._
import cats.syntax.functor._
import $organization$.api.{ApiModule, ApiModuleLoader}
import $organization$.persistence.{PersistenceModule, PersistenceModuleLoader}
import $organization$.processing.{ProcessingModule, ProcessingModuleLoader}
import $organization$.util.error.ErrorHandle
import $organization$.util.logging.TraceProvider
import com.typesafe.config.{Config, ConfigFactory}

class ApplicationLoader[F[_]: Timer: ContextShift: TraceProvider: ErrorHandle, G[_]: Sync](implicit F: Concurrent[F]) {

  import ApplicationLoader._

  def loadApplication(): Resource[F, Application[G]] = {
    for {
      config            <- Resource.liftF(loadConfig())
      persistenceModule <- persistenceModuleLoader(config).loadPersistenceModule()
      processingModule  <- processingModuleLoader(config, persistenceModule).loadProcessingModule()
      apiModule         <- Resource.liftF(apiModuleLoader(config).loadApiModule())

      application = Application(
        persistenceModule = persistenceModule,
        processingModule = processingModule,
        apiModule = apiModule
      )
    } yield application
  }

  protected def loadConfig(): F[Config] = Sync[F].delay(ConfigFactory.load())

  protected def persistenceModuleLoader(config: Config): PersistenceModuleLoader[F] = {
    new PersistenceModuleLoader[F](config)
  }

  protected def apiModuleLoader(config: Config): ApiModuleLoader[F, G] = {
    new ApiModuleLoader(config)
  }

  protected def processingModuleLoader(config: Config, persistenceModule: PersistenceModule): ProcessingModuleLoader[F] = {
    new ProcessingModuleLoader(config, persistenceModule)
  }

}

object ApplicationLoader {

  final case class Application[G[_]](
      persistenceModule: PersistenceModule,
      processingModule: ProcessingModule,
      apiModule: ApiModule[G]
  )

}
