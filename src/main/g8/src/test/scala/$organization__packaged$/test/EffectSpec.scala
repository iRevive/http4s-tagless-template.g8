package $organization$.test

import cats.data.EitherT
import cats.effect.Concurrent
import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import $organization$.util.error.ErrorIdGen
import $organization$.util.execution.Traced
import $organization$.util.logging.TraceId
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.{Inside, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration._

abstract class EffectSpec[E]
    extends AnyWordSpecLike
    with Matchers
    with EitherMatchers
    with OptionValues
    with Inside
    with EitherValues
    with ScalaCheckPropertyChecks { self =>

  protected type Eff[A] = Traced[EitherT[Task, E, *], A]

  protected implicit final val DefaultScheduler: Scheduler = monix.execution.Scheduler.Implicits.global
  protected implicit lazy val Eff: Concurrent[Eff]         = Concurrent.catsKleisliConcurrent
  protected implicit final val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const("test")

  protected final val DefaultTimeout: FiniteDuration = 20.seconds

  object EffectAssertion {

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def apply[A](timeout: Duration = DefaultTimeout)(program: Eff[A]): Unit =
      (for {
        traceId <- TraceId.randomAlphanumeric[Task](self.getClass.getSimpleName)
        result  <- program.run(traceId).void.value
      } yield result).runSyncUnsafe(timeout).value

  }

}
