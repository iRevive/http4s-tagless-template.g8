package $organization$.test

import cats.effect.ConcurrentEffect
import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import $organization$.util.{ClassUtils, TracedResultT}
import $organization$.util.logging.TraceId
import monix.execution.Scheduler
import eu.timepit.refined.types.string.NonEmptyString
import org.scalatest._

import scala.concurrent.duration._

trait BaseSpec extends WordSpecLike with Matchers with EitherMatchers with EitherValues with OptionValues with Inside {

  type Eff[A] = TracedResultT[A]
  protected implicit lazy val Eff: ConcurrentEffect[Eff]  = $organization$.util.concurrentEffect
  protected implicit lazy val DefaultScheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  protected val traceId: TraceId = TraceId.randomUuid()
  protected val DefaultTimeout   = 20.seconds

  protected def randomNonEmptyString(): NonEmptyString = NonEmptyString.unsafeFrom(randomString())
  protected def randomString(): String                 = scala.util.Random.alphanumeric.take(10).map(_.toLower).mkString

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
