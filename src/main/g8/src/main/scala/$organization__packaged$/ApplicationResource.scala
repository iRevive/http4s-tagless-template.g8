package $organization$

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.reducible._
import $organization$.ApplicationResource.{ApiModule, Application}
import $organization$.persistence.{PersistenceModule, PersistenceModuleResource}
import $organization$.service.{ServiceModule, ServiceModuleResource}
import $organization$.util.api._
import $organization$.util.error.{ErrorHandle, ErrorIdGen}
import $organization$.util.logging.TraceProvider
import $organization$.util.syntax.config._
import com.typesafe.config.Config
import org.http4s.server.middleware.{Logger => Http4sLogger}
import org.http4s.syntax.kleisli._
import org.http4s.{AuthedRoutes, HttpApp, HttpRoutes}
import io.odin.Logger
import io.odin.syntax._

class ApplicationResource[F[_]: Concurrent: TraceProvider: ErrorHandle: ErrorIdGen: Logger](
    persistenceModuleResource: PersistenceModuleResource[F],
    serviceModuleResource: ServiceModuleResource[F]
) {

  def create(config: Config): Resource[F, Application[F]] =
    for {
      blocker           <- Blocker[F]
      persistenceModule <- persistenceModuleResource.create(config, blocker)
      serviceModule     <- serviceModuleResource.create(config, persistenceModule)
      apiModule         <- Resource.liftF(createApiModule(config, persistenceModule, serviceModule))
    } yield Application(persistenceModule, serviceModule, apiModule)

  private def createApiModule(
      config: Config,
      persistenceModule: PersistenceModule[F],
      serviceModule: ServiceModule[F]
  ): F[ApiModule[F]] =
    for {
      apiConfig <- config.loadF[F, ApiConfig]("application.api")
      _         <- logger.info(render"Loading API module with config \$apiConfig")
    } yield ApiModule(mkRoutes(apiConfig.auth, persistenceModule, serviceModule), apiConfig)

  private def mkRoutes(
      authConfig: BasicAuthConfig,
      persistenceModule: PersistenceModule[F],
      serviceModule: ServiceModule[F]
  ): HttpApp[F] = {
    import $organization$.service.user.api.UserValidationErrorResponse._

    val healthApi = new HealthApi[F](persistenceModule.transactor)

    val serviceApi = NonEmptyList.of(serviceModule.userApi.routes).reduceK
    val secured    = AuthUtils.basicAuth[F](authConfig).apply(AuthedRoutes[Unit, F](req => serviceApi(req.req)))

    val allRoutes = NonEmptyList.of(healthApi.routes, secured).reduceK

    val middleware: HttpRoutes[F] => HttpRoutes[F] = { http: HttpRoutes[F] =>
      Http4sLogger.httpRoutes[F](logHeaders = true, logBody = true, logAction = Some(v => logger.debug(v)))(http)
    }.andThen(http => ErrorHandler.httpRoutes(logger)(http))
      .andThen(http => CorrelationIdTracer.httpRoutes(http))
      .andThen(http => org.http4s.server.middleware.CORS(http))

    middleware(allRoutes).orNotFound
  }

  private val logger: Logger[F] = Logger[F]

}

object ApplicationResource {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider: ErrorIdGen: Logger]: ApplicationResource[F] =
    new ApplicationResource[F](
      PersistenceModuleResource.default,
      new ServiceModuleResource[F]
    )

  final case class ApiModule[F[_]](httpApp: HttpApp[F], config: ApiConfig)

  final case class Application[F[_]](
      persistenceModule: PersistenceModule[F],
      serviceModule: ServiceModule[F],
      apiModule: ApiModule[F]
  )

}
