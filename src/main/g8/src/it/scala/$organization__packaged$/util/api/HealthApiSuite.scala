package $organization$.util.api

import $organization$.it.AppSuite
import org.http4s.syntax.literals.*
import org.http4s.{Method, Request, Status}

object HealthApiSuite extends AppSuite {

  test("return 'I'm alive' from /health endpoint") { app =>
    val request = Request[Eff](method = Method.GET, uri = uri"/health")
    val expected =
      """{"health":"Health.Healthy","checks":[{"tag":"postgres","health":"Health.Healthy"},{"tag":"api","health":"Health.Healthy"}]}"""

    for {
      response <- app.api.httpApp.run(request)
      body     <- response.as[String]
    } yield expect.all(response.status == Status.Ok, body == expected)
  }

}
