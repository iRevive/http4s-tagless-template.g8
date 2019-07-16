package $organization$.util.api

import cats.ApplicativeError
import cats.data.{Kleisli, OptionT}
import cats.syntax.applicativeError._
import cats.syntax.functor._
import $organization$.util.error.RaisedError
import $organization$.util.logging.TracedLogger
import $organization$.util.syntax.logging._
import io.circe.syntax._
import org.http4s.circe.jsonEncoder
import org.http4s.{HttpRoutes, Request, Response, Status}

object HttpUnhandledErrorHandler {

  def httpRoutes[F[_]: ApplicativeError[?[_], Throwable]](logger: TracedLogger[F])(routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes
          .run(req)
          .value
          .handleErrorWith { error =>
            val errorId  = RaisedError.generateErrorId
            val body     = ApiResponse.Error("Unhandled internal error", errorId).asJson
            val response = Response[F](Status.InternalServerError).withEntity(body)

            logger.error(log"Execution completed with an unhandled error \$error", error).as(Option(response))
          }
      }
    }

}
