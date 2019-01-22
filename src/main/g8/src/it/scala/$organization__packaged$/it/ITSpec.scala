package $organization$.it

import cats.arrow.FunctionK
import cats.effect.Concurrent
import cats.mtl.implicits._
import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import cats.~>
import $organization$.ApplicationLoader
import $organization$.ApplicationLoader.Application
import $organization$.util.logging.TraceId
import $organization$.util.syntax.mapK._
import $organization$.util.{ClassUtils, TracedLike, TracedResultT}
import eu.timepit.refined.types.string.NonEmptyString
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest._

import scala.concurrent.duration._
import scala.util.Random

trait ITSpec extends WordSpecLike with Matchers with EitherValues with OptionValues with EitherMatchers with Inside {

  type Eff[A] = TracedResultT[Task, A]
  protected implicit val Eff: Concurrent[Eff] = Concurrent.catsKleisliConcurrent

  protected implicit val DefaultScheduler: Scheduler = monix.execution.Scheduler.Implicits.global
  protected implicit val traceId: TraceId            = TraceId(ClassUtils.getClassSimpleName(getClass))

  protected val DefaultApplicationLoader = new ApplicationLoader[Eff, Task]

  protected def DefaultTimeout: FiniteDuration         = 20.seconds
  protected def randomNonEmptyString(): NonEmptyString = NonEmptyString.unsafeFrom(randomString())
  protected def randomString(): String                 = Random.alphanumeric.take(10).map(_.toLower).mkString

  protected implicit val taskToTask: Task ~> Task  = FunctionK.id
  protected implicit val effectToTask: Eff ~> Task = TracedLike[Eff, Task].arrow(traceId)

  protected def withApplication[F[_], A](
      timeout: Duration = DefaultTimeout
  )(program: Application[Task] => F[A])(implicit transformer: F ~> Task): Unit = {
    DefaultApplicationLoader
      .loadApplication()
      .mapK(effectToTask)
      .use(app => program(app).mapK(transformer))
      .void
      .runSyncUnsafe(timeout)
  }

  object EffectAssertion {

    def apply[F[_], A](timeout: Duration = DefaultTimeout)(program: F[A])(implicit transformer: F ~> Task): Unit = {
      program.mapK(transformer).void.runSyncUnsafe(timeout)
    }

  }

}
