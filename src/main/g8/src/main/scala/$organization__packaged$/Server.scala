package $organization$

import java.time.Instant

import cats.effect._
import cats.effect.syntax.bracket._
import cats.mtl.implicits._
import cats.syntax.functor._
import $organization$.ApplicationLoader.Application
import $organization$.util._
import $organization$.util.error.{ErrorHandle, RaisedError}
import $organization$.util.syntax.logging._
import $organization$.util.logging.{TraceId, TraceProvider, TracedLogger}
import com.typesafe.config.ConfigFactory
import eu.timepit.refined.auto.autoUnwrap
import monix.eval.{Task, TaskApp}
import org.http4s.server.blaze.BlazeServerBuilder

object Server extends TaskApp {

  override def run(args: List[String]): Task[ExitCode] = {
    implicit val CE: ConcurrentEffect[TracedResultT] = $organization$.util.concurrentEffect

    new Runner[TracedResultT]
      .serve(ApplicationLoader.default)
      .run(TraceId(s"Startup-\${Instant.now}"))
      .leftSemiflatMap(e => Task.raiseError(e.toException))
      .merge
  }

}

class Runner[F[_]: ConcurrentEffect: Timer: ContextShift: TraceProvider: ErrorHandle] {

  def serve(loader: ApplicationLoader[F]): F[ExitCode] =
    startApp(loader)
      .use(_ => Async[F].never[ExitCode])
      .handleWith[RaisedError](e => logger.error(log"Application start completed with error. \$e", e).as(ExitCode.Error))
      .guaranteeCase(finalizer)

  def startApp(applicationLoader: ApplicationLoader[F]): Resource[F, Unit] =
    for {
      _      <- Resource.liftF(logger.info("Starting the [$name$] service"))
      config <- Resource.liftF(Sync[F].delay(ConfigFactory.load()))
      app    <- applicationLoader.load(config)
      _      <- startApi(app)
    } yield ()

  private def startApi(app: Application[F]): Resource[F, Unit] = {
    val apiConfig = app.apiModule.config

    for {
      _ <- Resource.liftF(logger.info(log"Application trying to bind to host [\${apiConfig.host}:\${apiConfig.port}]"))
      _ <- BlazeServerBuilder[F].bindHttp(apiConfig.port, apiConfig.host).withHttpApp(app.apiModule.routes).resource
      _ <- Resource.liftF(logger.info(log"Application bound to [\${apiConfig.host}:\${apiConfig.port}]"))
    } yield ()
  }

  private def finalizer: ExitCase[Throwable] => F[Unit] = {
    case ExitCase.Completed =>
      logger.info("Application start completed")

    case ExitCase.Error(error) =>
      logger.error(log"Error during initialization of application. \$error", error)

    case ExitCase.Canceled =>
      logger.info("Application start was canceled")
  }

  private val logger = TracedLogger.create[F](getClass)

}
