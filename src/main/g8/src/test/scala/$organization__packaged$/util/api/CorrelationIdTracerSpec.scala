package $organization$.util.api

import cats.effect.concurrent.MVar
import cats.mtl.implicits._
import cats.syntax.functor._
import $organization$.test.BaseSpec
import $organization$.util.logging.TraceId./
import $organization$.util.logging.{TraceId, TraceProvider}
import org.http4s.Method._
import org.http4s._
import org.http4s.syntax.literals._

class CorrelationIdTracerSpec extends BaseSpec {

  "CorrelationIdTracer" should {

    "add `X-Correlation-Id` header to trace prefix" in EffectAssertion() {
      forAll { correlationId: String =>
        val header  = Header(CorrelationIdTracer.CorrelationIdHeader.value, correlationId)
        val request = Request[Eff](Method.GET, uri"/api/balance-state/123", headers = Headers.of(header))

        for {
          m       <- MVar.empty[Eff, TraceId]
          _       <- CorrelationIdTracer.httpRoutes[Eff](contextRecorder(m)).run(request).value
          traceId <- m.read
        } yield {
          inside(traceId) {
            case TraceId.ApiRoute(route) / TraceId.Const(value) / TraceId.Alphanumeric(_) =>
              route shouldBe "/api/balance-state"
              value shouldBe correlationId
          }
        }
      }
    }

    "use api route if `X-Correlation-Id` header is missing" in EffectAssertion() {
      val request = Request[Eff](Method.GET, uri"/api/balance-state/123")

      for {
        m       <- MVar.empty[Eff, TraceId]
        _       <- CorrelationIdTracer.httpRoutes[Eff](contextRecorder(m)).run(request).value
        traceId <- m.read
      } yield {
        inside(traceId) {
          case TraceId.ApiRoute(route) / TraceId.Alphanumeric(_) =>
            route shouldBe "/api/balance-state"
        }
      }
    }

  }

  private def contextRecorder(m: MVar[Eff, TraceId]): HttpRoutes[Eff] = {
    import org.http4s.dsl.impl.{->, /, Root}

    HttpRoutes.of[Eff] {
      case GET -> Root / "api" / "balance-state" / _ =>
        TraceProvider[Eff].ask.flatMap(m.put).as(Response[Eff](Status.Ok))
    }
  }

}
