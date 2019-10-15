package $organization$.util.api

import $organization$.test.BaseSpec
import org.http4s._
import org.http4s.syntax.kleisli._
import org.http4s.syntax.literals._

class HealthApiSpec extends BaseSpec {

  "Health API" should {

    "return 'I'm alive' from /health endpoint" in EffectAssertion() {
      val request = Request[Eff](method = Method.GET, uri = uri"/")

      for {
        response <- routes.orNotFound.run(request)
        body     <- response.as[String]
      } yield {
        response.status shouldBe Status.Ok
        body shouldBe "I'm alive"
      }
    }

  }

  private lazy val routes: HttpRoutes[Eff] = new HealthApi[Eff].routes

}
