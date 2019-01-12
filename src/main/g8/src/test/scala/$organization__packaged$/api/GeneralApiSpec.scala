package $organization$.api

import $organization$.test.BaseSpec
import monix.eval.Task
import org.http4s.{HttpRoutes, Method, Request, Status, Uri}
import org.http4s.syntax.kleisli._

class GeneralApiSpec extends BaseSpec {

  "General API" should {

    "return 'I'm alive' from api/health endpoint" in TaskAssertion {
      val request = Request[Task](method = Method.GET, uri = Uri.uri("/health"))

      for {
        response <- routes.orNotFound.run(request)
        body     <- response.as[String]
      } yield {
        response.status shouldBe Status.Ok
        body shouldBe "I'm alive"
      }
    }

  }

  private lazy val routes: HttpRoutes[Task] = new GeneralApi[Task]().routes

}
