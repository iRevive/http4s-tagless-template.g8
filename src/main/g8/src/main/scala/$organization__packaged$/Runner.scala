package $organization$

import cats.data.{EitherT, Kleisli}
import cats.effect._
import cats.effect.syntax.bracket._
import cats.mtl.implicits._
import cats.syntax.flatMap._
import $organization$.ApplicationResource.Application
import $organization$.util.error.{ErrorHandle, ErrorIdGen, RaisedError}
import $organization$.util.execution.{Eff, EffConcurrentEffect}
import $organization$.util.logging.RenderInstances._
import $organization$.util.logging.{Loggers, TraceId, TraceProvider}
import $organization$.util.syntax.logging._
import io.odin.{Level, Logger}
import io.odin.syntax._
import com.typesafe.config.ConfigFactory
import monix.eval.{Task, TaskApp}

class Runner[F[_]: Sync: TraceProvider: ErrorHandle: Logger] {

  final def run(appResource: ApplicationResource[F], job: Kleisli[F, Application[F], ExitCode]): F[ExitCode] =
    loadApp(appResource)
      .use(job.run)
      .handleWith[RaisedError](e => logger.error(render"Job completed with an error. \$e", e) >> e.raise[F, ExitCode])
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
      logger.error(render"Error during job execution. \$error", error)

    case ExitCase.Canceled =>
      logger.info("Job was canceled")
  }

  private final val logger: Logger[F] = Logger[F]

}

object Runner {

  trait Default extends TaskApp {

    implicit val Eff: ConcurrentEffect[Eff]  = new EffConcurrentEffect
    implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.alphanumeric(6)

    override final def run(args: List[String]): Task[ExitCode] =
      (for {
        traceId <- TraceId.randomAlphanumeric[EitherT[Task, RaisedError, *]](name)
        result  <- execute.run(traceId)
      } yield result).leftSemiflatMap(e => Task.raiseError(e.toException)).merge

    private final def execute: Eff[ExitCode] =
      Loggers
        .createContextLogger(Level.Info)
        .use(implicit logger => new Runner[Eff].run(ApplicationResource.default, job))

    def name: String
    def job: Kleisli[Eff, Application[Eff], ExitCode]

  }

}
