package $organization$

import cats.data.Kleisli
import cats.effect._
import cats.effect.syntax.bracket._
import cats.mtl.implicits._
import cats.syntax.functor._
import $organization$.ApplicationLoader.Application
import $organization$.util.TracedResultT
import $organization$.util.error.{ErrorHandle, RaisedError}
import $organization$.util.logging.{TraceId, TraceProvider, TracedLogger}
import $organization$.util.syntax.logging._
import com.typesafe.config.ConfigFactory
import monix.eval.{Task, TaskApp}

class Runner[F[_]: Sync: TraceProvider: ErrorHandle] {

  final def run(loader: ApplicationLoader[F], job: Kleisli[F, Application[F], ExitCode]): F[ExitCode] =
    loadApp(loader)
      .use(job.run)
      .handleWith[RaisedError](e => logger.error(log"Job completed with an error. \$e", e).as(ExitCode.Error))
      .guaranteeCase(finalizer)

  private final def loadApp(loader: ApplicationLoader[F]): Resource[F, Application[F]] =
    for {
      _      <- Resource.liftF(logger.info("Running the job"))
      config <- Resource.liftF(Sync[F].delay(ConfigFactory.load()))
      app    <- loader.load(config)
    } yield app

  private final def finalizer: ExitCase[Throwable] => F[Unit] = {
    case ExitCase.Completed =>
      logger.info("Job completed")

    case ExitCase.Error(error) =>
      logger.error(log"Error during job execution. \$error", error)

    case ExitCase.Canceled =>
      logger.info("Job was canceled")
  }

  private final val logger: TracedLogger[F] = TracedLogger.create[F](getClass)

}

object Runner {

  trait Default extends TaskApp {

    type F[A] = TracedResultT[A]
    implicit val F: ConcurrentEffect[F] = $organization$.util.concurrentEffect

    override final def run(args: List[String]): Task[ExitCode] =
      new Runner[F]
        .run(ApplicationLoader.default[F], job)
        .run(TraceId.randomAlphanumeric(name))
        .leftSemiflatMap(e => Task.raiseError(e.toException))
        .merge

    def name: String
    def job: Kleisli[F, Application[F], ExitCode]

  }

}
