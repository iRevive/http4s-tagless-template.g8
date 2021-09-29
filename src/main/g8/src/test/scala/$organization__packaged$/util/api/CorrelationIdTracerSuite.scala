package $organization$.util.api

import cats.effect.kernel.Deferred
import cats.syntax.functor.*
import $organization$.test.SimpleEffSuite
import $organization$.util.trace.TraceId./
import $organization$.util.trace.{TraceId, TraceProvider}
import org.http4s.Method.*
import org.http4s.*
import org.http4s.syntax.literals.*

object CorrelationIdTracerSuite extends SimpleEffSuite {

  test("add `X-Correlation-Id` header to trace prefix") {
    forall { (correlationId: String) =>
      val header  = Header.Raw(CorrelationIdTracer.CorrelationIdHeader, correlationId)
      val request = Request[Eff](Method.GET, uri"/api/endpoint/123", headers = Headers(header))

      for {
        d       <- Deferred[Eff, TraceId]
        _       <- CorrelationIdTracer.httpRoutes[Eff](contextRecorder(d)).run(request).value
        traceId <- d.get
      } yield traceId match {
        case TraceId.ApiRoute(route) / TraceId.Const(value) / TraceId.Alphanumeric(_) =>
          expect.all(route == "api/endpoint", value == correlationId)

        case other =>
          failure(s"The structure is incorrect \$traceId")
      }
    }
  }

  test("use api route if `X-Correlation-Id` header is missing") {
    val request = Request[Eff](Method.GET, uri"/api/endpoint/123")

    for {
      d       <- Deferred[Eff, TraceId]
      _       <- CorrelationIdTracer.httpRoutes[Eff](contextRecorder(d)).run(request).value
      traceId <- d.get
    } yield traceId match {
      case TraceId.ApiRoute(route) / TraceId.Alphanumeric(_) => expect(route == "api/endpoint")
      case other                                             => failure(s"The structure is incorrect \$traceId")
    }
  }

  private def contextRecorder(d: Deferred[Eff, TraceId]): HttpRoutes[Eff] = {
    import org.http4s.dsl.impl.{->, /}
    import org.http4s.Uri.Path.Root

    HttpRoutes.of[Eff] { case GET -> Root / "api" / "endpoint" / _ =>
      TraceProvider[Eff].ask.flatMap(ctx => d.complete(ctx.traceId)).as(Response[Eff](Status.Ok))
    }
  }

}
