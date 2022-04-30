package $organization$.util.api

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.mtl.implicits.*
import $organization$.util.trace.{LogContext, TraceId, TraceProvider}
import org.http4s.syntax.string.*
import org.http4s.{Header, HttpRoutes, Request}
import org.typelevel.ci.*

object CorrelationIdTracer {

  val CorrelationIdHeader: CIString = CIString("X-Correlation-Id")

  def httpRoutes[F[_]: Sync: TraceProvider](routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { (req: Request[F]) =>
      val path              = req.uri.path.normalize.toString
      val lastSlashIdx      = path.lastIndexOf("/")
      val route             = if (lastSlashIdx > 0) path.substring(0, lastSlashIdx) else path
      val correlationHeader = req.headers.get(CorrelationIdHeader).map(_.head.value)
      val root: TraceId     = TraceId.ApiRoute(route)

      for {
        alphanumeric <- OptionT.liftF(TraceId.randomAlphanumeric[F](6))
        traceId = correlationHeader.fold(root)(v => root.child(TraceId.Const(v))).child(alphanumeric)
        header  = Header.Raw(CorrelationIdHeader, correlationHeader.getOrElse(alphanumeric.value))
        outerContext <- OptionT.liftF(TraceProvider[F].ask)
        result       <- routes.run(req).map(_.putHeaders(header)).scope(LogContext(traceId, outerContext.extra))
      } yield result
    }

}
