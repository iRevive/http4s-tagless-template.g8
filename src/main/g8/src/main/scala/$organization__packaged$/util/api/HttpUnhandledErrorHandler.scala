package $organization$.util.api

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import $organization$.util.error.RaisedError
import $organization$.util.logging.TracedLogger
import $organization$.util.syntax.logging._
import io.circe.syntax._
import org.http4s.circe.jsonEncoder
import org.http4s.{HttpRoutes, Request, Response, Status}

object HttpUnhandledErrorHandler {

  def httpRoutes[F[_]: Sync](logger: TracedLogger[F])(routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes
          .run(req)
          .value
          .handleErrorWith { error =>
            for {
              errorId  <- RaisedError.generateErrorId
              body     <- ApiResponse.Error("Unhandled internal error", errorId).asJson.pure[F]
              response <- Response[F](Status.InternalServerError).withEntity(body).pure[F]
              _        <- logger.error(log"Execution completed with an unhandled error \$error", error)
            } yield Option(response)
          }
      }
    }

}
