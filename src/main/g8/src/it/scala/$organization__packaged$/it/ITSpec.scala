package $organization$.it

import cats.effect.ConcurrentEffect
import cats.mtl.implicits._
import cats.scalatest.{EitherMatchers, EitherValues}
import cats.syntax.functor._
import $organization$.ApplicationResource
import $organization$.ApplicationResource.Application
import $organization$.util.ClassUtils
import $organization$.util.error.ErrorIdGen
import $organization$.util.execution.EffConcurrentEffect
import $organization$.util.logging.TraceId
import com.typesafe.config.{Config, ConfigFactory}
import monix.eval.Task
import monix.execution.Scheduler
import org.scalacheck.Arbitrary
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import io.estatico.newtype.Coercible

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
  protected implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const("test")

  protected def DefaultTimeout: FiniteDuration = 20.seconds

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  protected def withApplication[A](timeout: Duration = DefaultTimeout)(program: Application[Eff] => Eff[A]): Unit =
    EffectAssertion(timeout) {
      ApplicationResource.default[Eff].create(DefaultConfig).use(program)
    }

  object EffectAssertion {

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def apply[A](timeout: Duration = DefaultTimeout)(program: Eff[A]): Unit =
      (for {
        traceId <- TraceId.randomAlphanumeric[Task](className)
        result  <- program.run(traceId).void.value
      } yield result).runSyncUnsafe(timeout).value

  }

  protected implicit def coercibleArbitrary[R, N](
      implicit ev: Coercible[Arbitrary[R], Arbitrary[N]],
      R: Arbitrary[R]
  ): Arbitrary[N] =
    ev(R)

  protected lazy val DefaultConfig: Config = ConfigFactory.load()

  private lazy val className: String = ClassUtils.getClassSimpleName(getClass)

}
