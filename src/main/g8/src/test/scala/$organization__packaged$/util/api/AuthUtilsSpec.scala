package $organization$.util.api

import $organization$.test.BaseSpec
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.syntax.kleisli._
import org.http4s.syntax.literals._

class AuthUtilsSpec extends BaseSpec {

  "AuthUtils" should {

    "pass request with valid credentials" in EffectAssertion() {
      val config = BasicAuthConfig("realm", "user", "pwd")

      val header  = Authorization(BasicCredentials(config.user, config.password))
      val request = Request[Eff](Method.GET, uri"/api/endpoint", headers = Headers.of(header))

      for {
        response <- AuthUtils.basicAuth(config).apply(routes).orNotFound.run(request)
      } yield response.status shouldBe Status.Ok
    }

    "reject request with invalid credentials" in EffectAssertion() {
      val config  = BasicAuthConfig("realm", "user", "pwd")
      val header  = Authorization(BasicCredentials("random", "random-pwd"))
      val request = Request[Eff](Method.GET, uri"/api/endpoint", headers = Headers.of(header))

      for {
        response <- AuthUtils.basicAuth(config).apply(routes).orNotFound.run(request)
      } yield response.status shouldBe Status.Unauthorized
    }

  }

  private lazy val routes: AuthedRoutes[Unit, Eff] = {
    object dsl extends Http4sDsl[Eff]
    import dsl._

    AuthedRoutes.of[Unit, Eff] { case Method.GET -> Root / "api" / "endpoint" as _ =>
      Eff.pure(Response[Eff]())
    }
  }

}
