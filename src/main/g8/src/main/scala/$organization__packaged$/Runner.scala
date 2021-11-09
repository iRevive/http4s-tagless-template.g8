package $organization$

import cats.data.Kleisli
import cats.effect.syntax.monadCancel.*
import cats.effect.{Async, ExitCode, IO, IOApp, Outcome, Resource}
import cats.mtl.implicits.*
import cats.syntax.flatMap.*
import cats.syntax.monoid.*
import $organization$.util.ConfigSource
import $organization$.util.error.{ErrorChannel, ErrorIdGen}
import $organization$.util.execution.{Eff, WithError}
import $organization$.util.instances.render.*
import $organization$.util.logging.Loggers
import $organization$.util.trace.{LogContext, TraceId, TraceProvider}
import com.typesafe.config.ConfigFactory
import io.odin.syntax.*
import io.odin.{Level, Logger}

object Runner {

  trait Simple extends Shared {
    type Res = Application[Eff]

    override final def acquireResource(logger: Logger[Eff]): Resource[Eff, Application[Eff]] = {
      implicit val log: Logger[Eff]                = logger
      implicit val errorChannel: ErrorChannel[Eff] = ErrorChannel.create(ErrorIdGen.alphanumeric(6))

      for {
        config <- Resource.eval(ConfigSource.fromTypesafeConfig)
        app    <- Application.create(config)
      } yield app
    }
  }

  trait Shared extends IOApp {

    type Eff[A] = $organization$.util.execution.Eff[A]
    type Res

    implicit val effect: Async[Eff] = Async.asyncForKleisli

    def name: String
    def acquireResource(logger: Logger[Eff]): Resource[Eff, Res]
    def job(resource: Res): Eff[ExitCode]

    override final def run(args: List[String]): IO[ExitCode] =
      (for {
        traceId <- TraceId.randomAlphanumeric[WithError](name, length = 6)
        result  <- createLogger.use(logger => execute(logger)).run(LogContext(traceId, Map.empty))
      } yield result).leftSemiflatMap(e => IO.raiseError(e.toException)).merge

    private final def execute(logger: Logger[Eff]): Eff[ExitCode] =
      acquireResource(logger).use(job).guaranteeCase(finalizer(logger))

    private final def createLogger: Resource[Eff, Logger[Eff]] = {
      val logFile      = s"logs/\$name.log"
      val consoleLevel = Loggers.envLogLevel("LOG_LEVEL").getOrElse(Level.Info)
      val fileLevel    = Loggers.envLogLevel("FILE_LOG_LEVEL").getOrElse(Level.Error)

      val console = Loggers.consoleContextLogger[Eff](Level.Info)

      for {
        _      <- Resource.eval(console.info(render"Using console logger with level \$consoleLevel."))
        _      <- Resource.eval(console.info(render"Using file logger with level \$fileLevel. Output \$logFile."))
        logger <- Loggers.consoleContextLoggerAsync(consoleLevel) |+| Loggers.fileContextLoggerAsync(logFile, fileLevel)
      } yield logger
    }

    private final def finalizer(logger: Logger[Eff]): Outcome[Eff, Throwable, ExitCode] => Eff[Unit] = {
      case Outcome.Succeeded(_)   => logger.info("Job completed")
      case Outcome.Errored(error) => logger.error("Error during job execution", error)
      case Outcome.Canceled()     => logger.info("Job was canceled")
    }

  }

}
