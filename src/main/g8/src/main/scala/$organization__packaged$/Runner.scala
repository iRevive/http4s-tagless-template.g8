package $organization$

import cats.data.{EitherT, Kleisli}
import cats.effect._
import cats.effect.syntax.bracket._
import cats.mtl.implicits._
import cats.syntax.flatMap._
import cats.syntax.monoid._
import $organization$.ApplicationResource.Application
import $organization$.util.error.{ErrorHandle, ErrorIdGen, RaisedError}
import $organization$.util.execution.{Eff, EffConcurrentEffect}
import $organization$.util.instances.render._
import $organization$.util.logging.{Loggers, TraceId, TraceProvider}
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
      createLogger.use(implicit logger => new Runner[Eff].run(ApplicationResource.default, job))

    private final def createLogger: Resource[Eff, Logger[Eff]] = {
      val logFile      = s"logs/\$name.log"
      val consoleLevel = Loggers.envLogLevel("LOG_LEVEL").getOrElse(Level.Info)
      val fileLevel    = Loggers.envLogLevel("FILE_LOG_LEVEL").getOrElse(Level.Error)

      val console = Loggers.consoleContextLogger[Eff](Level.Info)

      for {
        _      <- Resource.liftF(console.error(render"Using console logger with level \$consoleLevel."))
        _      <- Resource.liftF(console.error(render"Using file logger with level \$fileLevel. Output \$logFile."))
        logger <- Loggers.consoleContextLoggerAsync(consoleLevel) |+| Loggers.fileContextLoggerAsync(logFile, fileLevel)
      } yield logger
    }

    def name: String
    def job: Kleisli[Eff, Application[Eff], ExitCode]

  }

}
