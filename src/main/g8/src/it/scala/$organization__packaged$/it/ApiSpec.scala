package $organization$.it

import cats.mtl.implicits._
import $organization$.ApplicationResource.Application
import $organization$.util.api.ApiResponse
import $organization$.util.syntax.json._
import io.circe.{Decoder, DecodingFailure, Json}
import org.http4s.{BasicCredentials, Headers, Method, Request, Response, Status, Uri}
import org.http4s.headers.Authorization
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.Assertion

import scala.reflect.ClassTag

trait ApiSpec extends ITSpec {

  def authorizedApi(method: Method, uri: Uri): Unit = {

    s"fail in case of missing auth header (\$uri)" in withApplication() { app =>
      val request = Request[Eff](method, uri)

      for {
        response <- app.apiModule.httpApp.run(request)
      } yield response.status shouldBe Status.Unauthorized
    }

    s"fail in case of invalid auth credentials (\$uri)" in withApplication() { app =>
      val authHeader = Authorization(Arbitrary.arbitrary[BasicCredentials].sample.value)
      val request    = Request[Eff](method, uri, headers = Headers.of(authHeader))

      for {
        response <- app.apiModule.httpApp.run(request)
      } yield response.status shouldBe Status.Unauthorized
    }

  }

  def metaResponseForInvalidJson(method: Method, uri: Uri): Unit = {

    s"return 500 InternalServerError if request body is empty (\$method \$uri)" in withApplication() { implicit app =>
      val request = withAuth(Request[Eff](method, uri))

      for {
        (response, body) <- executeRequest[ApiResponse.Error](request)
      } yield {
        response.status shouldBe Status.InternalServerError
        body shouldBe ApiResponse.Error("Unhandled internal error", body.errorId)
      }
    }

    s"return 400 BadRequest if request body is invalid (\$method \$uri)" in withApplication() { implicit app =>
      import io.circe.syntax._
      import org.http4s.circe.jsonEncoder

      val body    = Json.obj(Gen.alphaNumStr.sample.value := Gen.alphaNumStr.sample.value)
      val request = withAuth(Request[Eff](method, uri).withEntity(body))

      for {
        (response, _) <- executeRequest[ApiResponse.Error](request)
      } yield {
        response.status shouldBe Status.BadRequest
      }
    }

  }

  protected def executeAndCheck[A: Decoder: ClassTag](
      request: Request[Eff],
      expectedStatus: Status,
      expectedBody: A
  )(implicit app: Application[Eff]): Eff[Assertion] =
    for {
      (response, body) <- executeRequest[A](request)
    } yield {
      response.status shouldBe expectedStatus
      body shouldBe expectedBody
    }

  protected def executeRequest[A: Decoder: ClassTag](
      request: Request[Eff]
  )(implicit app: Application[Eff]): Eff[(Response[Eff], A)] = {
    import org.http4s.circe.jsonDecoder

    for {
      response <- app.apiModule.httpApp.run(request)
      body     <- response.as[Json]
      entity   <- body.decodeF[Eff, A]
    } yield (response, entity)
  }

  protected def withAuth(request: Request[Eff])(implicit app: Application[Eff]): Request[Eff] = {
    val authConfig = app.apiModule.config.auth
    request.putHeaders(Authorization(BasicCredentials(authConfig.user, authConfig.password)))
  }

  protected implicit def successResponseDecoder[A: Decoder]: Decoder[ApiResponse.Success[A]] =
    Decoder.instance { v =>
      for {
        isSuccess <- v.get[Boolean]("success")
        _         <- Either.cond(isSuccess, (), DecodingFailure("Field 'success' must be 'true'", v.history))
        result    <- v.get[A]("result")
      } yield ApiResponse.Success(result)
    }

  protected implicit val errorResponseDecoder: Decoder[ApiResponse.Error] =
    Decoder.instance { v =>
      for {
        isSuccess <- v.get[Boolean]("success")
        _         <- Either.cond(!isSuccess, (), DecodingFailure("Field 'success' must be 'false'", v.history))
        error     <- v.get[String]("error")
        errorId   <- v.get[String]("errorId")
      } yield ApiResponse.Error(error, errorId)
    }

  private implicit val basicCredentialsArbitrary: Arbitrary[BasicCredentials] =
    Arbitrary {
      for {
        username <- Gen.alphaNumStr
        password <- Gen.alphaNumStr
      } yield BasicCredentials(username, password)
    }

}
