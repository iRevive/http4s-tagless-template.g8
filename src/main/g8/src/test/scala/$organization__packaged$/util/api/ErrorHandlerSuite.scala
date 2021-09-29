package $organization$.util.api

import cats.mtl.Handle.handleKleisli
import cats.mtl.implicits.*
import $organization$.persistence.postgres.PostgresError
import $organization$.test.SimpleEffSuite
import $organization$.util.error.{AppError, ErrorChannel, ErrorIdGen, RaisedError}
import $organization$.util.logging.Loggers
import io.circe.syntax.*
import io.odin.meta.Position
import io.odin.{Level, Logger}
import org.http4s.*
import org.http4s.circe.*
import org.http4s.headers.`Content-Type`
import org.http4s.syntax.kleisli.*
import org.http4s.syntax.literals.*

object ErrorHandlerSuite extends SimpleEffSuite {

  import $organization$.service.user.api.UserValidationErrorResponse.*

  test("return result") {
    val defaultResponse = Response[Eff]().withEntity(ApiResponse.Success("value").asJson)
    val routes          = mkRoutes(Eff.pure(defaultResponse))
    val middleware      = ErrorHandler.httpRoutes[Eff](routes).orNotFound

    for {
      response <- middleware.run(Request[Eff](Method.GET, uri"/api/endpoint"))
      body     <- response.as[String]
    } yield {
      val expectedBody = """{"success":true,"result":"value"}"""

      expect.all(
        response.status == Status.Ok,
        response.contentType == Some(`Content-Type`(MediaType.application.json)),
        body == expectedBody
      )
    }
  }

  test("handle checked error") {
    val error = RaisedError(
      PostgresError.ConnectionAttemptTimeout("error"),
      Position.derivePosition,
      "test"
    )

    val routes     = mkRoutes(error.raise[Eff, Response[Eff]])
    val middleware = ErrorHandler.httpRoutes[Eff](routes).orNotFound

    for {
      response <- middleware.run(Request[Eff](Method.GET, uri"/api/endpoint"))
      body     <- response.as[String]
    } yield {
      val expectedBody = """{"success":false,"error":"Postgres connection timeout","errorId":"test"}"""

      expect.all(
        response.status == Status.BadRequest,
        response.contentType == Some(`Content-Type`(MediaType.application.json)),
        body == expectedBody
      )
    }
  }

  test("handled exception") {
    val error      = new RuntimeException("Something went wrong"): Throwable
    val routes     = mkRoutes(Eff.raiseError(error))
    val middleware = ErrorHandler.httpRoutes[Eff](routes).orNotFound

    for {
      response <- middleware.run(Request[Eff](Method.GET, uri"/api/endpoint"))
      body     <- response.as[String]
    } yield {
      val expectedBody = """{"success":false,"error":"Unhandled internal error","errorId":"test"}"""

      expect.all(
        response.status == Status.InternalServerError,
        response.contentType == Some(`Content-Type`(MediaType.application.json)),
        body == expectedBody
      )
    }
  }

  private def mkRoutes(response: Eff[Response[Eff]]): HttpRoutes[Eff] = {
    import org.http4s.dsl.impl.{->, /}
    import org.http4s.Uri.Path.Root

    HttpRoutes.of[Eff] { case Method.GET -> Root / "api" / "endpoint" =>
      response
    }
  }

  private implicit val logger: Logger[Eff] =
    Loggers.consoleContextLogger(Level.Info)

  private implicit val errorChannel: ErrorChannel[Eff] =
    ErrorChannel.create(ErrorIdGen.const("test"))

  private implicit val appErrorResponse: ErrorResponseSelector[F, AppError] =
    AppError.appErrorResponse[F]

}
