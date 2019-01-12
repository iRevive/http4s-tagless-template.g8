package $organization$.test

import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import $organization$.util.logging.TraceId
import monix.eval.Task
import monix.execution.Scheduler
import eu.timepit.refined.types.string.NonEmptyString
import org.scalatest._

import scala.concurrent.duration._

trait BaseSpec extends WordSpecLike with Matchers with EitherMatchers with EitherValues with OptionValues with Inside {

  protected implicit val DefaultScheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  protected val traceId: TraceId = TraceId.randomUuid()
  protected val DefaultTimeout   = 20.seconds

  protected def randomNonEmptyString(): NonEmptyString = NonEmptyString.unsafeFrom(randomString())
  protected def randomString(): String                 = scala.util.Random.alphanumeric.take(10).map(_.toLower).mkString

  object TaskAssertion {
    def apply[A](flow: Task[A]): Unit = flow.void.runSyncUnsafe(DefaultTimeout)
  }

}
