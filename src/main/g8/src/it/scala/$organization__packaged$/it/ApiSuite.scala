package $organization$.it

import cats.mtl.Handle.handleKleisli
import $organization$.Application
import $organization$.util.api.ApiResponse
import $organization$.util.syntax.json.*
import io.circe.{Decoder, DecodingFailure, Json}
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, Headers, Method, Request, Response, Status as HttpStatus, Uri}
import org.scalacheck.{Arbitrary, Gen}
import weaver.Expectations

import scala.reflect.ClassTag

trait ApiSuite extends AppSuite {

  def authorizedApi(method: Method, uri: Uri): Unit = {

    test(s"fail in case of missing auth header (\$uri)") { app =>
      val request = Request[Eff](method, uri)

      for {
        response <- app.api.httpApp.run(request)
      } yield expect(response.status == HttpStatus.Unauthorized)
    }

    test(s"fail in case of invalid auth credentials (\$uri)") { app =>
      val authHeader = Authorization(Arbitrary.arbitrary[BasicCredentials].sample.get)
      val request    = Request[Eff](method, uri, headers = Headers(authHeader))

      for {
        response <- app.api.httpApp.run(request)
      } yield expect(response.status == HttpStatus.Unauthorized)
    }

  }

  def metaResponseForInvalidJson(method: Method, uri: Uri): Unit = {

    test(s"return 500 InternalServerError if request body is empty (\$method \$uri)") { implicit app =>
      val request = withAuth(Request[Eff](method, uri))

      for {
        (response, body) <- executeRequest[ApiResponse.Error](request)
      } yield expect.all(
        response.status == HttpStatus.InternalServerError,
        body == ApiResponse.Error("Unhandled internal error", body.errorId)
      )
    }

    test(s"return 400 BadRequest if request body is invalid (\$method \$uri)") { implicit app =>
      import io.circe.syntax.*
      import org.http4s.circe.jsonEncoder

      val body    = Json.obj(Gen.alphaNumStr.sample.get := Gen.alphaNumStr.sample.get)
      val request = withAuth(Request[Eff](method, uri).withEntity(body))

      for {
        (response, _) <- executeRequest[ApiResponse.Error](request)
      } yield expect(response.status == HttpStatus.BadRequest)
    }

  }

  protected def executeAndCheck[A: Decoder: ClassTag](
      request: Request[Eff],
      expectedStatus: HttpStatus,
      expectedBody: A
  )(implicit app: Application[Eff]): Eff[Expectations] =
    for {
      (response, body) <- executeRequest[A](request)
    } yield expect.all(response.status == expectedStatus, body == expectedBody)

  protected def executeRequest[A: Decoder: ClassTag](
      request: Request[Eff]
  )(implicit app: Application[Eff]): Eff[(Response[Eff], A)] = {
    import org.http4s.circe.jsonDecoder

    for {
      response <- app.api.httpApp.run(request)
      body     <- response.as[Json]
      entity   <- body.decodeF[Eff, A]
    } yield (response, entity)
  }

  protected def withAuth(request: Request[Eff])(implicit app: Application[Eff]): Request[Eff] = {
    val authConfig = app.api.config.auth
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
