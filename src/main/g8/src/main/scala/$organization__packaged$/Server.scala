package $organization$

import cats.effect._
import cats.mtl.implicits._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import $organization$.ApplicationLoader.Application
import $organization$.util._
import $organization$.util.error.ErrorHandle
import $organization$.util.logging.Loggable.InterpolatorOps._
import $organization$.util.logging.{TraceId, TraceProvider, TracedLogger}
import $organization$.util.syntax.resources._
import eu.timepit.refined.auto.autoUnwrap
import monix.eval.{Task, TaskApp}
import org.http4s.server.blaze.BlazeServerBuilder

import scala.util.control.NonFatal

object Server extends TaskApp {

  type Effect[A] = TracedResultT[Task, A]

  override def run(args: List[String]): Task[ExitCode] = {
    val traceId = TraceId(s"Startup-\${TimeUtils.zonedDateTimeNow()}")

    new Runner[Effect, Task].run(traceId)
  }

}

class Runner[F[_]: Concurrent: Timer: ContextShift: TraceProvider: ErrorHandle, G[_]: ConcurrentEffect](
    implicit tracedLike: TracedLike[F, G]
) {

  def run(traceId: TraceId): G[ExitCode] = {
    startApp(new ApplicationLoader[F, G], traceId)
      .use[ExitCode](_ => Async[G].never)
      .recoverWith { case NonFatal(e) => onError(e).run(traceId) }
  }

  def startApp(applicationLoader: ApplicationLoader[F, G], traceId: TraceId): Resource[G, Unit] = {
    for {
      _   <- Resource.liftF(logger.info("Starting the [$name$] service").run(traceId))
      app <- applicationLoader.loadApplication().mapK(tracedLike.transformer(traceId))
      _   <- startApi(app, traceId)
    } yield ()
  }

  private def startApi(app: Application[G], traceId: TraceId): Resource[G, Unit] = {
    val apiConfig = app.apiModule.config

    for {
      _ <- Resource.liftF(logger.info(log"Application trying to bind to host [\${apiConfig.host}:\${apiConfig.port}]").run(traceId))
      _ <- BlazeServerBuilder[G].bindHttp(apiConfig.port, apiConfig.host).withHttpApp(app.apiModule.routes).resource
      _ <- Resource.liftF(logger.info(log"Application bound to [\${apiConfig.host}:\${apiConfig.port}]").run(traceId))
    } yield ()
  }

  private def onError(unhandledError: Throwable): Traced[G, ExitCode] = {
    logger.error(log"Error during initialization of application. \$unhandledError", unhandledError) >>
      Sync[Traced[G, ?]].pure(ExitCode.Error)
  }

  private val logger = TracedLogger.create[Traced[G, ?]](getClass)

}
