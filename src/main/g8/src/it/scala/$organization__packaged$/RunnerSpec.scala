package $organization$

import cats.effect.syntax.concurrent._
import cats.mtl.implicits._
import cats.syntax.applicativeError._
import $organization$.it.ITSpec
import $organization$.util.error.ErrorHandle

import scala.concurrent.duration._

class RunnerSpec extends ITSpec {

  "Runner" should {

    "start application" in EffectAssertion(40.seconds) {
      val loader    = DefaultApplicationLoader
      val runner    = new Runner[Eff]

      for {
        result <- ErrorHandle[Eff].attempt(runner.startApp(loader).use(_ => Eff.unit))
      } yield result should beRight(())
    }

    "return error if process was cancel" in EffectAssertion() {
      for {
        fiber         <- new Runner[Eff].serve(ApplicationLoader.default).start
        cancelAttempt <- fiber.cancel.attempt
      } yield cancelAttempt should beRight(())
    }

  }

}
