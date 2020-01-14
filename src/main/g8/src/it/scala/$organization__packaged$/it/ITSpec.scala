package $organization$.it

import cats.effect.ConcurrentEffect
import cats.effect.testing.scalatest.scalacheck.EffectCheckerAsserting
import cats.mtl.implicits._
import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import $organization$.ApplicationResource
import $organization$.ApplicationResource.Application
import $organization$.util.error.ErrorIdGen
import $organization$.util.execution.EffConcurrentEffect
import $organization$.util.logging.{Loggers, TraceId}
import com.typesafe.config.{Config, ConfigFactory}
import io.estatico.newtype.Coercible
import io.odin.Level
import monix.eval.Task
import monix.execution.Scheduler
import org.scalacheck.Arbitrary
import org.scalatest.{Inside, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.{CheckerAsserting, ScalaCheckPropertyChecks}

import scala.concurrent.duration._

trait ITSpec
    extends AnyWordSpecLike
    with Matchers
    with EitherValues
    with OptionValues
    with EitherMatchers
    with Inside
    with ScalaCheckPropertyChecks { self =>

  protected type Eff[A] = $organization$.util.execution.Eff[A]

  protected implicit final val DefaultScheduler: Scheduler = monix.execution.Scheduler.Implicits.global
  protected implicit final val Eff: ConcurrentEffect[Eff]  = new EffConcurrentEffect
  protected implicit final val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const("test")

  protected final val DefaultTimeout: FiniteDuration = 20.seconds

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  protected def withApplication[A](timeout: Duration = DefaultTimeout)(program: Application[Eff] => Eff[A]): Unit =
    EffectAssertion(timeout) {
      Loggers
        .createContextLogger(Level.Info)
        .flatMap(implicit logger => ApplicationResource.default[Eff].create(DefaultConfig))
        .use(program)
    }

  object EffectAssertion {

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def apply[A](timeout: Duration = DefaultTimeout)(program: Eff[A]): Unit =
      (for {
        traceId <- TraceId.randomAlphanumeric[Task](self.getClass.getSimpleName)
        result  <- program.run(traceId).void.value
      } yield result).runSyncUnsafe(timeout).value

  }

  protected implicit def coercibleArbitrary[R, N](
      implicit ev: Coercible[Arbitrary[R], Arbitrary[N]],
      R: Arbitrary[R]
  ): Arbitrary[N] =
    ev(R)

  protected lazy final val DefaultConfig: Config = ConfigFactory.load()

  protected implicit final def effCheckingAsserting[A]: CheckerAsserting[Eff[A]] { type Result = Eff[Unit] } =
    new EffectCheckerAsserting

}
