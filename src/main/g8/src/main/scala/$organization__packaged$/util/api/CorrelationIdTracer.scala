package $organization$.util.api

import cats.Monad
import cats.data.Kleisli
import cats.mtl.implicits._
import $organization$.util.logging.{TraceId, TraceProvider}
import $organization$.util.syntax.logging._
import org.http4s.syntax.string._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, HttpRoutes, Request}

object CorrelationIdTracer {

  val CorrelationIdHeader: CaseInsensitiveString = "X-Correlation-Id".ci

  def httpRoutes[F[_]: Monad: TraceProvider](routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      val route             = req.uri.path.substring(0, req.uri.path.lastIndexOf("/"))
      val correlationHeader = req.headers.get(CorrelationIdHeader)
      val traceId           = TraceId.randomAlphanumeric(tracePrefix(correlationHeader, route))

      routes
        .run(req)
        .map(_.putHeaders(correlationHeader.toList: _*))
        .scope(traceId)
    }

  private def tracePrefix(correlationHeader: Option[Header], route: String): String =
    correlationHeader match {
      case Some(h) =>
        log"\$route#\${h.value}"

      case None =>
        route + "#"
    }

}
