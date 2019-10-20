package $organization$

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import $organization$.ApplicationLoader.{ApiModule, Application}
import $organization$.persistence.{PersistenceModule, PersistenceModuleLoader}
import $organization$.service.{ServiceModule, ServiceModuleLoader}
import $organization$.util.api.{ApiConfig, HealthApi}
import $organization$.util.error.{ErrorHandle, ErrorIdGen}
import $organization$.util.logging.{TraceProvider, TracedLogger}
import $organization$.util.syntax.config._
import $organization$.util.syntax.logging._
import com.typesafe.config.Config
import org.http4s.HttpApp
import org.http4s.server.Router
import org.http4s.syntax.kleisli._

class ApplicationLoader[F[_]: Sync: TraceProvider: ErrorHandle: ErrorIdGen](
    persistenceModuleLoader: PersistenceModuleLoader[F],
    serviceModuleLoader: ServiceModuleLoader[F]
) {

  def load(config: Config): Resource[F, Application[F]] =
    for {
      blocker           <- Blocker[F]
      persistenceModule <- persistenceModuleLoader.load(config, blocker)
      serviceModule     <- serviceModuleLoader.load(config, persistenceModule)
      apiModule         <- Resource.liftF(loadApiModule(config, serviceModule))
    } yield Application(persistenceModule, serviceModule, apiModule)

  private def loadApiModule(config: Config, serviceModule: ServiceModule[F]): F[ApiModule[F]] =
    for {
      apiConfig <- config.loadF[F, ApiConfig]("application.api")
      _         <- logger.info(log"Loading API module with config \$apiConfig")
    } yield ApiModule(mkRoutes(serviceModule), apiConfig)

  private def mkRoutes(serviceModule: ServiceModule[F]): HttpApp[F] = {
    val _         = serviceModule
    val healthApi = new HealthApi[F]

    Router(
      "/health" -> healthApi.routes
    ).orNotFound
  }

  private val logger: TracedLogger[F] = TracedLogger.create[F](getClass)

}

object ApplicationLoader {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider: ErrorIdGen]: ApplicationLoader[F] =
    new ApplicationLoader[F](
      PersistenceModuleLoader.default,
      new ServiceModuleLoader[F]
    )

  final case class ApiModule[F[_]](httpApp: HttpApp[F], config: ApiConfig)

  final case class Application[F[_]](
      persistenceModule: PersistenceModule[F],
      serviceModule: ServiceModule[F],
      apiModule: ApiModule[F]
  )

}
