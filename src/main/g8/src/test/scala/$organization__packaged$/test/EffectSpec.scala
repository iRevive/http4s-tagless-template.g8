package $organization$.test

import cats.data.EitherT
import cats.effect.Concurrent
import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import $organization$.util.ClassUtils
import $organization$.util.error.ErrorIdGen
import $organization$.util.execution.Traced
import $organization$.util.logging.TraceId
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.{Inside, Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration._

abstract class EffectSpec[E]
    extends WordSpecLike
    with Matchers
    with EitherMatchers
    with OptionValues
    with Inside
    with EitherValues
    with ScalaCheckPropertyChecks {

  protected type Eff[A] = Traced[EitherT[Task, E, *], A]

  protected implicit val DefaultScheduler: Scheduler = monix.execution.Scheduler.Implicits.global
  protected implicit val Eff: Concurrent[Eff]        = Concurrent.catsKleisliConcurrent
  protected implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const("test")

  protected final val DefaultTimeout: FiniteDuration = 20.seconds

  object EffectAssertion {

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def apply[A](timeout: Duration = DefaultTimeout)(program: Eff[A]): Unit =
      (for {
        traceId <- TraceId.randomAlphanumeric[Task](className)
        result  <- program.run(traceId).void.value
      } yield result).runSyncUnsafe(timeout).value

  }

  private lazy val className: String = ClassUtils.getClassSimpleName(getClass)

}
