package $organization$

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import $organization$.ApplicationLoader.{ApiModule, Application}
import $organization$.persistence.{PersistenceModule, PersistenceModuleLoader}
import $organization$.processing.{ProcessingModule, ProcessingModuleLoader}
import $organization$.util.api.{ApiConfig, HealthApi}
import $organization$.util.error.ErrorHandle
import $organization$.util.logging.{TraceProvider, TracedLogger}
import $organization$.util.syntax.config._
import $organization$.util.syntax.logging._
import com.typesafe.config.Config
import monix.execution.Scheduler
import org.http4s.HttpApp
import org.http4s.server.Router
import org.http4s.syntax.kleisli._

class ApplicationLoader[F[_]: Sync: TraceProvider: ErrorHandle](
    persistenceModuleLoader: PersistenceModuleLoader[F],
    processingModuleLoader: ProcessingModuleLoader[F]
) {

  def load(config: Config): Resource[F, Application[F]] =
    for {
      blocker           <- makeBlocker
      persistenceModule <- persistenceModuleLoader.load(config, blocker)
      processingModule  <- processingModuleLoader.load(config, persistenceModule)
      apiModule         <- Resource.liftF(loadApiModule(config, processingModule))
    } yield Application(persistenceModule, processingModule, apiModule)

  private def loadApiModule(config: Config, processingModule: ProcessingModule[F]): F[ApiModule[F]] =
    for {
      apiConfig <- config.loadF[F, ApiConfig]("application.api")
      _         <- logger.info(log"Loading API module with config \$apiConfig")
    } yield ApiModule(mkRoutes(processingModule), apiConfig)

  private def mkRoutes(processingModule: ProcessingModule[F]): HttpApp[F] = {
    val _         = processingModule
    val healthApi = new HealthApi[F]

    Router(
      "/health" -> healthApi.routes
    ).orNotFound
  }

  private def makeBlocker: Resource[F, Blocker] =
    Resource
      .make(Sync[F].delay(Scheduler.io()))(s => Sync[F].delay(s.shutdown()))
      .map(Blocker.liftExecutionContext)

  private val logger: TracedLogger[F] = TracedLogger.create[F](getClass)

}

object ApplicationLoader {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider]: ApplicationLoader[F] =
    new ApplicationLoader[F](
      PersistenceModuleLoader.default,
      new ProcessingModuleLoader[F]
    )

  final case class ApiModule[F[_]](httpApp: HttpApp[F], config: ApiConfig)

  final case class Application[F[_]](
      persistenceModule: PersistenceModule[F],
      processingModule: ProcessingModule[F],
      apiModule: ApiModule[F]
  )

}
