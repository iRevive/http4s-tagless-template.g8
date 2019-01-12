package $organization$

import cats.mtl.implicits._
import $organization$.it.ITSpec
import $organization$.util.logging.TraceId
import monix.eval.Task

import scala.concurrent.duration._

class RunnerSpec extends ITSpec {

  "Runner" should {

    "start application" ignore {
      val loader    = DefaultApplicationLoader
      val runner    = new Runner[Eff, Task]
      val startFlow = runner.startApp(loader, TraceId.randomUuid()).use(_ => Task.unit)

      noException shouldBe thrownBy(startFlow.runSyncUnsafe(40.seconds))
    }

    "return error if process was cancel" in {
      val fiber = new Runner[Eff, Task].run(traceId).start.runSyncUnsafe()

      noException shouldBe thrownBy(fiber.cancel.runSyncUnsafe(DefaultTimeout))
    }

  }

}
