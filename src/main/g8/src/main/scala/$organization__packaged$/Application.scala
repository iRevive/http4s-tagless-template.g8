package $organization$

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.reducible.*
import $organization$.api.Api
import $organization$.persistence.Persistence
import $organization$.service.Services
import $organization$.util.ConfigSource
import $organization$.util.api.*
import $organization$.util.error.{AppError, ErrorChannel, ErrorIdGen}
import $organization$.util.json.JsonDecodingError
import $organization$.util.trace.TraceProvider
import com.typesafe.config.Config
import io.odin.Logger
import io.odin.syntax.*
import org.http4s.server.middleware.{CORS, Logger as Http4sLogger}
import org.http4s.syntax.kleisli.*
import org.http4s.{AuthedRoutes, HttpApp, HttpRoutes, Response}

final case class Application[F[_]](
    persistence: Persistence[F],
    services: Services[F],
    api: Api[F],
    logger: Logger[F]
)

object Application {

  def create[F[_]: Async: ErrorChannel: TraceProvider: Logger](config: ConfigSource[F]): Resource[F, Application[F]] =
    for {
      persistence <- Persistence.create(config)
      services    <- Services.create(config, persistence)
      api         <- Resource.eval(Api.create(config, persistence, services))
    } yield Application(persistence, services, api, Logger[F])

}
