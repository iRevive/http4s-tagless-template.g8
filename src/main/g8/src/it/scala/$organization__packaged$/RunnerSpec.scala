package $organization$

import cats.data.Kleisli
import cats.effect.ExitCode
import cats.mtl.implicits._
import $organization$.it.ITSpec

import scala.concurrent.duration._

class RunnerSpec extends ITSpec {

  "Runner" should {

    "start application" in EffectAssertion(40.seconds) {
      val runner = new Runner[Eff]

      for {
        result <- runner.run(ApplicationResource.default[Eff], Kleisli.pure(ExitCode.Success))
      } yield result shouldBe ExitCode.Success
    }

  }

}
