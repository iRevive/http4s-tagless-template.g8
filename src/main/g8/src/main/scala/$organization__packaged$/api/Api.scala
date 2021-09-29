package $organization$.api

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.reducible.*
import $organization$.persistence.Persistence
import $organization$.service.Services
import $organization$.service.user.api.UserValidationErrorResponse.*
import $organization$.util.ConfigSource
import $organization$.util.api.{ApiConfig, AuthUtils, BasicAuthConfig, CorrelationIdTracer, ErrorResponseSelector, HealthApi}
import $organization$.util.error.{AppError, ErrorChannel}
import $organization$.util.trace.TraceProvider
import io.odin.Logger
import io.odin.syntax.*
import org.http4s.syntax.kleisli.*
import org.http4s.{AuthedRoutes, HttpApp}

final case class Api[F[_]](httpApp: HttpApp[F], config: ApiConfig)

object Api {

  def create[F[_]: Async: Logger: ErrorChannel: TraceProvider](
      config: ConfigSource[F],
      persistence: Persistence[F],
      services: Services[F]
  ): F[Api[F]] =
    for {
      cfg <- config.get[ApiConfig]("application.api")
      _   <- Logger[F].info(render"Loading API module with config \$cfg")
    } yield {
      val healthApi = new HealthApi[F](persistence.transactor)

      val serviceApi = NonEmptyList.of(services.userApi.routes).reduceK
      val secured    = AuthUtils.basicAuth[F](cfg.auth).apply(AuthedRoutes[Unit, F](req => serviceApi(req.req)))

      val allRoutes = NonEmptyList.of(healthApi.routes, secured).reduceK

      implicit val appErrorResponse: ErrorResponseSelector[F, AppError] = AppError.appErrorResponse[F]

      Api(Middleware.httpRoutes(allRoutes).orNotFound, cfg)
    }

}
