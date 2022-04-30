package $organization$.api

import cats.effect.Async
import $organization$.util.api.ErrorHandler.AppErrorResponse
import $organization$.util.api.{CorrelationIdTracer, ErrorHandler}
import $organization$.util.error.ErrorChannel
import $organization$.util.trace.TraceProvider
import io.odin.Logger
import org.http4s.server.middleware.{CORS, Logger as Http4sLogger}
import org.http4s.{AuthedRoutes, HttpApp, HttpRoutes, Response}

object Middleware {

  def httpRoutes[F[_]: Async: ErrorChannel: TraceProvider: AppErrorResponse: Logger](
      routes: HttpRoutes[F]
  ): HttpRoutes[F] =
    CORS.policy(
      CorrelationIdTracer.httpRoutes(
        ErrorHandler.httpRoutes(
          Http4sLogger.httpRoutes(logHeaders = true, logBody = true, logAction = Some(v => Logger[F].debug(v)))(routes)
        )
      )
    )

}
