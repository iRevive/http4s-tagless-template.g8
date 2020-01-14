package $organization$.util.api

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import cats.syntax.applicativeError._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.mtl.syntax.handle._
import $organization$.util.error.{AppError, ErrorHandle, ErrorIdGen, RaisedError}
import $organization$.util.logging.TracedLogger
import $organization$.util.syntax.logging._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.{HttpRoutes, Request, Response, Status}

object ErrorHandler {

  type MonadThrow[F[_]]       = MonadError[F, Throwable]
  type AppErrorResponse[F[_]] = ErrorResponseSelector[F, AppError]

  def httpRoutes[F[_]: MonadThrow: ErrorIdGen: ErrorHandle: AppErrorResponse](logger: TracedLogger[F])(
      routes: HttpRoutes[F]
  ): HttpRoutes[F] = {

    def onMonadError(error: Throwable): F[Option[Response[F]]] =
      for {
        errorId  <- ErrorIdGen[F].gen
        body     <- ApiResponse.Error("Unhandled internal error", errorId).asJson.pure[F]
        response <- Response[F](Status.InternalServerError).withEntity(body).pure[F]
        _        <- logger.error(log"Execution completed with an unhandled error \$error. Error ID [\$errorId]", error)
      } yield Option(response)

    def onApplicativeHandleError(error: RaisedError): F[Option[Response[F]]] =
      for {
        _ <- logger.error(log"Execution completed with an error \$error", error)
      } yield Option(ErrorResponseSelector[F, AppError].toResponse(error.error, error.errorId))

    Kleisli { req: Request[F] =>
      OptionT {
        routes
          .run(req)
          .value
          .handleErrorWith(onMonadError)
          .handleWith[RaisedError](onApplicativeHandleError)
      }
    }
  }

}
