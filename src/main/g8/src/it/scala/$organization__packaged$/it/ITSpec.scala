package $organization$.it

import cats.effect.ConcurrentEffect
import cats.mtl.implicits._
import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import $organization$.ApplicationLoader
import $organization$.ApplicationLoader.Application
import $organization$.util.ClassUtils
import $organization$.util.execution.EffConcurrentEffect
import $organization$.util.logging.TraceId
import com.typesafe.config.{Config, ConfigFactory}
import eu.timepit.refined.types.string.NonEmptyString
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest._
import org.scalatestplus.scalacheck.{CheckerAsserting, ScalaCheckPropertyChecks}

import scala.concurrent.duration._

trait ITSpec
    extends WordSpecLike
    with Matchers
    with EitherValues
    with OptionValues
    with EitherMatchers
    with Inside
    with ScalaCheckPropertyChecks {

  protected type Eff[A] = $organization$.util.execution.Eff[A]

  protected implicit val DefaultScheduler: Scheduler = monix.execution.Scheduler.Implicits.global
  protected implicit val Eff: ConcurrentEffect[Eff]  = new EffConcurrentEffect

  protected val DefaultApplicationLoader = ApplicationLoader.default[Eff]

  protected def DefaultTimeout: FiniteDuration = 20.seconds

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  protected def withApplication[A](timeout: Duration = DefaultTimeout)(program: Application[Eff] => Eff[A]): Unit =
    EffectAssertion(timeout) {
      ApplicationLoader.default[Eff].load(DefaultConfig).use(program)
    }

  object EffectAssertion {

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def apply[A](timeout: Duration = DefaultTimeout)(program: Eff[A]): Unit =
      (for {
        traceId <- TraceId.randomAlphanumeric[Task](className)
        result  <- program.run(traceId).void.value
      } yield result).runSyncUnsafe(timeout).value

  }

  protected implicit def checkingAsserting[A]: CheckerAsserting[A] { type Result = Eff[Unit] } =
    new EffectCheckerAsserting[Eff, A]

  protected lazy val DefaultConfig: Config = ConfigFactory.load()

  private lazy val className: String = ClassUtils.getClassSimpleName(getClass)

}
