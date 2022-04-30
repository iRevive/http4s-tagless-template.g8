package $organization$.util.api

import $organization$.test.SimpleEffSuite
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.syntax.kleisli.*
import org.http4s.syntax.literals.*

object AuthUtilsSuite extends SimpleEffSuite {

  test("pass request with valid credentials") {
    val config = BasicAuthConfig("realm", "user", "pwd")

    val header  = Authorization(BasicCredentials(config.user, config.password))
    val request = Request[Eff](Method.GET, uri"/api/endpoint", headers = Headers(header))

    for {
      response <- AuthUtils.basicAuth(config).apply(routes).orNotFound.run(request)
    } yield expect(response.status == Status.Ok)
  }

  test("reject request with invalid credentials") {
    val config  = BasicAuthConfig("realm", "user", "pwd")
    val header  = Authorization(BasicCredentials("random", "random-pwd"))
    val request = Request[Eff](Method.GET, uri"/api/endpoint", headers = Headers(header))

    for {
      response <- AuthUtils.basicAuth(config).apply(routes).orNotFound.run(request)
    } yield expect(response.status == Status.Unauthorized)
  }

  private lazy val routes: AuthedRoutes[Unit, Eff] = {
    object dsl extends Http4sDsl[Eff]
    import dsl.*

    AuthedRoutes.of[Unit, Eff] { case Method.GET -> Root / "api" / "endpoint" as _ =>
      Eff.pure(Response[Eff]())
    }
  }

}
