package $organization$.util.api

import cats.mtl.implicits._
import $organization$.persistence.postgres.PostgresError
import $organization$.test.BaseSpec
import $organization$.util.Position
import $organization$.util.error.RaisedError
import $organization$.util.logging.TracedLogger
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.headers.`Content-Type`
import org.http4s.syntax.kleisli._
import org.http4s.syntax.literals._
import shapeless.syntax.inject._

class ErrorHandlerSpec extends BaseSpec {

  "ErrorHandler" should {

    "return result" in EffectAssertion() {
      val defaultResponse = Response[Eff]().withEntity(ApiResponse.Success("value").asJson)
      val routes          = mkRoutes(Eff.pure(defaultResponse))
      val middleware      = ErrorHandler.httpRoutes[Eff](TracedLogger.create(getClass))(routes).orNotFound

      for {
        response <- middleware.run(Request[Eff](Method.GET, uri"/api/endpoint"))
        body     <- response.as[String]
      } yield {
        val expectedBody = """{"success":true,"result":"value"}"""

        response.status shouldBe Status.Ok
        response.contentType shouldBe Some(`Content-Type`(MediaType.application.json))
        body shouldBe expectedBody
      }
    }

    "handle checked error" in EffectAssertion() {
      val error = RaisedError(
        PostgresError.connectionAttemptTimeout("error").inject,
        Position.generate,
        "test"
      )

      val routes     = mkRoutes(error.raise[Eff, Response[Eff]])
      val middleware = ErrorHandler.httpRoutes[Eff](TracedLogger.create(getClass))(routes).orNotFound

      for {
        response <- middleware.run(Request[Eff](Method.GET, uri"/api/endpoint"))
        body     <- response.as[String]
      } yield {
        val expectedBody = """{"success":false,"error":"Postgres connection timeout","errorId":"test"}"""

        response.status shouldBe Status.BadRequest
        response.contentType shouldBe Some(`Content-Type`(MediaType.application.json))
        body shouldBe expectedBody
      }
    }

    "handled exception" in EffectAssertion() {
      val error      = new RuntimeException("Something went wrong"): Throwable
      val routes     = mkRoutes(Eff.raiseError(error))
      val middleware = ErrorHandler.httpRoutes[Eff](TracedLogger.create(getClass))(routes).orNotFound

      for {
        response <- middleware.run(Request[Eff](Method.GET, uri"/api/endpoint"))
        body     <- response.as[String]
      } yield {
        val expectedBody = """{"success":false,"error":"Unhandled internal error","errorId":"test"}"""

        response.status shouldBe Status.InternalServerError
        response.contentType shouldBe Some(`Content-Type`(MediaType.application.json))
        body shouldBe expectedBody
      }
    }

  }

  private def mkRoutes(response: Eff[Response[Eff]]): HttpRoutes[Eff] = {
    import org.http4s.dsl.impl.{->, /, Root}

    HttpRoutes.of[Eff] {
      case Method.GET -> Root / "api" / "endpoint" =>
        response
    }
  }

}
