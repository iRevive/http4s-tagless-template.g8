package $organization$.it

import cats.effect.kernel.Resource
import cats.effect.syntax.monadCancel.*
import cats.mtl.syntax.local.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import $organization$.Application
import $organization$.test.MutableEffSuite
import $organization$.util.ConfigSource
import $organization$.util.error.{ErrorChannel, ErrorIdGen}
import $organization$.util.execution.Eff
import $organization$.util.logging.Loggers
import $organization$.util.trace.{LogContext, TraceId}
import io.odin.loggers.ContextualLogger
import io.odin.syntax.*
import io.odin.{Level, Logger}
import weaver.*
import weaver.scalacheck.{CheckConfig, Checkers}

import scala.concurrent.duration.*

trait AppSuite extends MutableEffSuite { self =>

  override type Res = Application[Eff]

  protected implicit final val errorChannel: ErrorChannel[Eff] = ErrorChannel.create(ErrorIdGen.alphanumeric(6))

  override def maxParallelism: Int = 1

  override def checkConfig: CheckConfig = CheckConfig(
    minimumSuccessful = 10,
    maximumDiscardRatio = 10,
    maximumGeneratorSize = 20,
    perPropertyParallelism = 1,
    initialSeed = None
  )

  override def sharedResource: Resource[Eff, Application[Eff]] =
    for {
      (given Logger[Eff]) <- Resource.eval(weaverLogger)
      config              <- Resource.eval(ConfigSource.fromTypesafeConfig[Eff])
      app                 <- Application.create(config)
    } yield app

  override def test(name: TestName): PartiallyAppliedTest = new ExtendedPartiallyAppliedTest(name)

  private def weaverLogger: Eff[Logger[Eff]] =
    for {
      logger <- WeaverLogger.create[Eff](Loggers.formatter, consoleLogger, Level.Info)
    } yield logger.withContext

  private def consoleLogger: Logger[Eff] =
    io.odin.consoleLogger[Eff](Loggers.formatter, Level.Error).withContext

  private final class ExtendedPartiallyAppliedTest(name: TestName) extends PartiallyAppliedTest(name) {
    override def apply(run: Res => Eff[Expectations]): Unit =
      registerTest(name)(res => Test(name.name, (log: Log[Eff]) => withWeaverLog(log, res)((res, _) => run(res))))

    override def apply(run: (Res, Log[Eff]) => Eff[Expectations]): Unit =
      registerTest(name)(res => Test(name.name, (log: Log[Eff]) => withWeaverLog(log, res)(run)))

    private def withWeaverLog(log: Log[Eff], res: Res)(run: (Res, Log[Eff]) => Eff[Expectations]): Eff[Expectations] =
      res.logger match {
        case ContextualLogger(weaverLogger: WeaverLogger[Eff]) =>
          val key = WeaverLogger.Key(name.name + "-" + scala.util.Random.alphanumeric.take(10).mkString)
          val ctx = LogContext(TraceId.Const(name.name), Map(WeaverLogger.ContextKey -> key.value))

          weaverLogger
            .setWeaverLog(key, log)
            .bracket(_ => run(res, log).scope(ctx))(_ => weaverLogger.removeWeaverLog(key))
            .scope(ctx)

        case other =>
          for {
            _      <- consoleLogger.error(s"Invalid application logger. Expected `Contextual(WeaverLogger)`, got \$other")
            result <- run(res, log)
          } yield result
      }
  }

}
