package $organization$.util.api

import cats.data.{Kleisli, OptionT}
import cats.mtl.syntax.handle._
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.{Applicative, Monad}
import $organization$.persistence.mongo.MongoError
import $organization$.persistence.postgres.PostgresError
import $organization$.util.config.ConfigParsingError
import $organization$.util.error.{AppError, ErrorHandle, RaisedError}
import $organization$.util.json.JsonDecodingError
import $organization$.util.syntax.logging._
import org.http4s.{HttpRoutes, Request}

object HttpErrorHandler {

  def httpRoutes[F[_]: Monad: ErrorHandle](routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes
          .run(req)
          .value
          .handleWith[RaisedError](e => ErrorResponseSelector[F, AppError].toResponse(e.error, e.errorId).pure[F].map(Option(_)))
      }
    }

  implicit def configParsingErrorResponse[F[_]: Applicative]: ErrorResponseSelector[F, ConfigParsingError] =
    ErrorResponseSelector.badRequestResponse(e => log"Cannot load config [\${e.expectedClass}] at [\${e.path}]")

  implicit def jsonDecodingErrorResponse[F[_]: Applicative]: ErrorResponseSelector[F, JsonDecodingError] =
    ErrorResponseSelector.badRequestResponse(e => log"Json decoding error. \${e.errors}")

  implicit def postgresErrorResponse[F[_]: Applicative]: ErrorResponseSelector[F, PostgresError] =
    ErrorResponseSelector.badRequestResponse {
      case PostgresError.UnavailableConnection(_)    => "Postgres connection is not available"
      case PostgresError.ConnectionAttemptTimeout(_) => "Postgres connection timeout"
    }

  implicit def mongoErrorResponse[F[_]: Applicative]: ErrorResponseSelector[F, MongoError] =
    ErrorResponseSelector.badRequestResponse {
      case MongoError.UnavailableConnection(_)    => "Mongo connection is not available"
      case MongoError.ConnectionAttemptTimeout(_) => "Mongo connection timeout"
      case MongoError.ExecutionError(_)           => "Cannot execute mongo query"
    }

}
