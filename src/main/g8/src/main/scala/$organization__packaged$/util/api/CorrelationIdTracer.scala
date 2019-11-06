package $organization$.util.api

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.mtl.implicits._
import $organization$.util.logging.{TraceId, TraceProvider}
import org.http4s.syntax.string._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, HttpRoutes, Request}

object CorrelationIdTracer {

  val CorrelationIdHeader: CaseInsensitiveString = "X-Correlation-Id".ci

  def httpRoutes[F[_]: Sync: TraceProvider](routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      val route             = req.uri.path.substring(0, req.uri.path.lastIndexOf("/"))
      val correlationHeader = req.headers.get(CorrelationIdHeader).map(_.value)
      val root: TraceId     = TraceId.ApiRoute(route)

      for {
        alphanumeric <- OptionT.liftF(TraceId.randomAlphanumeric[F](6))
        traceId = correlationHeader.fold(root)(v => root.child(TraceId.Const(v))).child(alphanumeric)
        header  = Header(CorrelationIdHeader.value, correlationHeader.getOrElse(alphanumeric.value))
        result <- routes.run(req).map(_.putHeaders(header)).scope(traceId)
      } yield result
    }

}
