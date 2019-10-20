package $organization$

import cats.data.{EitherT, Kleisli}
import cats.effect._
import cats.effect.syntax.bracket._
import cats.mtl.implicits._
import cats.syntax.functor._
import $organization$.ApplicationResource.Application
import $organization$.util.error.{ErrorHandle, ErrorIdGen, RaisedError}
import $organization$.util.execution.{Eff, EffConcurrentEffect}
import $organization$.util.logging.{TraceId, TraceProvider, TracedLogger}
import $organization$.util.syntax.logging._
import com.typesafe.config.ConfigFactory
import monix.eval.{Task, TaskApp}

class Runner[F[_]: Sync: TraceProvider: ErrorHandle] {

  final def run(appResource: ApplicationResource[F], job: Kleisli[F, Application[F], ExitCode]): F[ExitCode] =
    loadApp(appResource)
      .use(job.run)
      .handleWith[RaisedError](e => logger.error(log"Job completed with an error. \$e", e).as(ExitCode.Error))
      .guaranteeCase(finalizer)

  private final def loadApp(appResource: ApplicationResource[F]): Resource[F, Application[F]] =
    for {
      _      <- Resource.liftF(logger.info("Running the job"))
      config <- Resource.liftF(Sync[F].delay(ConfigFactory.load()))
      app    <- appResource.create(config)
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

    implicit val Eff: ConcurrentEffect[Eff]  = new EffConcurrentEffect
    implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.alphanumeric(6)

    override final def run(args: List[String]): Task[ExitCode] =
      (for {
        traceId <- TraceId.randomAlphanumeric[EitherT[Task, RaisedError, ?]](name)
        result  <- new Runner[Eff].run(ApplicationResource.default, job).run(traceId)
      } yield result).leftSemiflatMap(e => Task.raiseError(e.toException)).merge

    def name: String
    def job: Kleisli[Eff, Application[Eff], ExitCode]

  }

}
