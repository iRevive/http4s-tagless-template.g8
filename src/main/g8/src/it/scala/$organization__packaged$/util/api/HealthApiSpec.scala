package $organization$.util.api

import $organization$.it.ITSpec
import org.http4s._
import org.http4s.syntax.literals._

class HealthApiSpec extends ITSpec {

  "Health API" should {

    "return 'I'm alive' from /health endpoint" in withApplication() { app =>
      val request = Request[Eff](method = Method.GET, uri = uri"/health")
      val expected =
        """{"health":"Healthy","checks":[{"tag":"postgres","health":"Healthy"},{"tag":"api","health":"Healthy"}]}"""

      for {
        response <- app.apiModule.httpApp.run(request)
        body     <- response.as[String]
      } yield {
        response.status shouldBe Status.Ok
        body shouldBe expected
      }
    }

  }

}
