package $organization$.util.api

import cats.MonadThrow
import cats.data.{Kleisli, OptionT}
import cats.mtl.syntax.handle.*
import cats.syntax.applicative.*
import cats.syntax.applicativeError.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import $organization$.util.error.{AppError, ErrorChannel, RaisedError}
import $organization$.util.instances.render.*
import io.circe.syntax.*
import io.odin.Logger
import io.odin.syntax.*
import org.http4s.circe.*
import org.http4s.{HttpRoutes, Request, Response, Status}

object ErrorHandler {

  type AppErrorResponse[F[_]] = ErrorResponseSelector[F, AppError]

  def httpRoutes[F[_]: ErrorChannel: AppErrorResponse: Logger](routes: HttpRoutes[F]): HttpRoutes[F] = {
    implicit val monadThrow: MonadThrow[F] = ErrorChannel[F].monadThrow

    def onMonadError(error: Throwable): F[Option[Response[F]]] =
      for {
        errorId  <- ErrorChannel[F].errorIdGen.gen
        body     <- ApiResponse.Error("Unhandled internal error", errorId).asJson.pure[F]
        response <- Response[F](Status.InternalServerError).withEntity(body).pure[F]
        ctx      <- Map("error_id" -> errorId).pure[F]
        _        <- Logger[F].error(render"Execution completed with an unhandled error \$error", ctx, error)
      } yield Option(response)

    def onHandleError(error: RaisedError): F[Option[Response[F]]] =
      for {
        ctx <- Map("error_id" -> error.errorId).pure[F]
        _   <- Logger[F].error(render"Execution completed with an error \$error", ctx, error)
      } yield Option(ErrorResponseSelector[F, AppError].toResponse(error.error, error.errorId))

    Kleisli { (req: Request[F]) =>
      OptionT {
        routes
          .run(req)
          .value
          .handleErrorWith(onMonadError)
          .handleWith[RaisedError](onHandleError)(ErrorChannel[F].handle)
      }
    }
  }

}
