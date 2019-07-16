package $organization$.util.api

import cats.mtl.implicits._
import $organization$.test.{BaseSpec, GenRandom}
import $organization$.util.logging.TraceProvider
import org.http4s._

class CorrelationIdTracerSpec extends BaseSpec {

  "CorrelationIdTracer" should {

    "add `X-Correlation-Id` header to trace prefix" in {
      val correlationId = GenRandom[String].gen
      val header        = Header(CorrelationIdTracer.CorrelationIdHeader.value, GenRandom[String].gen)
      val request       = Request[Eff](Method.GET, uri"/route", headers = Headers.of(header))

      for {
        _       <- CorrelationIdTracer.httpRoutes[Eff](HttpRoutes.empty).run(request).value
        traceId <- TraceProvider[Eff].ask
      } yield traceId.value shouldBe s"api-/route-\$correlationId"
    }

    "use api route if `X-Correlation-Id` header is missing" in {
      val request = Request[Eff](Method.GET, uri"/route")
      for {
        _       <- CorrelationIdTracer.httpRoutes[Eff](HttpRoutes.empty).run(request).value
        traceId <- TraceProvider[Eff].ask
      } yield traceId.value shouldBe "api-/route"
    }

  }

}
