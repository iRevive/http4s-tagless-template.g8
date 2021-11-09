package $organization$.test

import cats.effect.{Async, IO, Resource}
import $organization$.util.execution.Eff
import fs2.Stream
import weaver.*
import weaver.scalacheck.Checkers

import scala.concurrent.duration.*

abstract class MutableEffSuite extends MutableFSuite[Eff] with BaseCatsSuite with Expectations.Helpers with Checkers { self =>

  protected implicit def effectCompat: EffUnsafeRun = new EffUnsafeRun(name)

  protected type Eff[A] = $organization$.util.execution.Eff[A]
  protected final val Eff: Async[Eff] = effect

  final def getSuite: EffectSuite[IO] = IOSuite

  object IOSuite extends SimpleMutableIOSuite {
    // sleep(1.second) -> See https://gitter.im/functional-streams-for-scala/fs2?at=5fdcc586c746c6431cc15391
    override def spec(args: List[String]): Stream[IO, TestOutcome] =
      self.spec(args).translate(self.effectCompat.toIO) ++ Stream.sleep_(1.second)
  }

}

abstract class SimpleEffSuite extends MutableEffSuite {
  type Res = Unit
  final def sharedResource: Resource[Eff, Unit] = Resource.unit
}
