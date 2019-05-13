package $organization$.api

import $organization$.test.BaseSpec
import org.http4s.syntax.kleisli._
import org.http4s._

class GeneralApiSpec extends BaseSpec {

  "General API" should {

    "return 'I'm alive' from /health endpoint" in EffectAssertion() {
      val request = Request[Eff](method = Method.GET, uri = uri"/health")

      for {
        response <- routes.orNotFound.run(request)
        body     <- response.as[String]
      } yield {
        response.status shouldBe Status.Ok
        body shouldBe "I'm alive"
      }
    }

  }

  private lazy val routes: HttpRoutes[Eff] = new GeneralApi[Eff].routes

}
