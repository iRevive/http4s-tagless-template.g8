package $organization$.test

import cats.data.EitherT
import cats.effect.Concurrent
import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import $organization$.util.logging.TraceId
import $organization$.util.{ClassUtils, Traced}
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.{Inside, Matchers, OptionValues, WordSpecLike}

import scala.concurrent.duration._

abstract class EffectSpec[E]
    extends WordSpecLike
    with Matchers
    with EitherMatchers
    with OptionValues
    with Inside
    with EitherValues {

  protected type Eff[A] = Traced[EitherT[Task, E, ?], A]
  protected implicit lazy val Eff: Concurrent[Eff]        = Concurrent.catsKleisliConcurrent
  protected implicit lazy val DefaultScheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  protected final val DefaultTimeout: FiniteDuration = 20.seconds

  object EffectAssertion {

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def apply[A](timeout: Duration = DefaultTimeout)(program: Eff[A]): Unit =
      program
        .run(TraceId.randomAlphanumeric(className))
        .void
        .value
        .runSyncUnsafe(timeout)
        .value

  }

  private lazy val className: String = ClassUtils.getClassSimpleName(getClass)

}
