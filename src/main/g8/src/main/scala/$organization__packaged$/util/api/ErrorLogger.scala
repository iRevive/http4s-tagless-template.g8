package $organization$.util.api

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.mtl.syntax.handle._
import cats.mtl.syntax.raise._
import cats.syntax.apply._
import $organization$.util.error.{ErrorHandle, RaisedError}
import $organization$.util.logging.TracedLogger
import $organization$.util.syntax.logging._
import org.http4s.{HttpRoutes, Request}

object ErrorLogger {

  def httpRoutes[F[_]: Sync: ErrorHandle, E](logger: TracedLogger[F])(routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes
          .run(req)
          .value
          .handleWith[RaisedError](e => logger.error(log"Execution completed with an error \$e", e) *> e.raise)
      }
    }

}
