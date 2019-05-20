package $organization$.util

import $organization$.test.BaseSpec

import scala.util.Try
import scala.util.control.NonFatal

class PositionSpec extends BaseSpec {

  "Position" should {

    "correctly extract enclosing class & method" in {
      val pkg = "$organization$.util"

      fakeMethod() shouldBe Position(s"\$pkg.PositionSpec#fakeMethod", 39)

      lambda() shouldBe Position(s"\$pkg.PositionSpec#lambda", 43)

      partialFunction().apply(()) shouldBe Position(s"\$pkg.PositionSpec#partialFunction", 47)

      recoverLambda() shouldBe Position(s"\$pkg.PositionSpec#recoverLambda", 51)

      val fakeClass = new FakeClass

      fakeClass.method() shouldBe Position(s"\$pkg.PositionSpec#FakeClass#method", 55)

      fakeClass.lambda() shouldBe Position(s"\$pkg.PositionSpec#FakeClass#lambda", 58)

      fakeClass.forComprehension() shouldBe Position(s"\$pkg.PositionSpec#FakeClass#forComprehension", 63)

      fakeClass.generatedPos() shouldBe Position(s"\$pkg.PositionSpec#FakeClass#generatedPos", 66)

      fakeClass.nestedPos() shouldBe Position(s"\$pkg.PositionSpec#FakeClass#nestedPos nested2", 73)

    }
  }

  def fakeMethod(): Position =
    Position.generate

  def lambda(): Position =
    Some("x").map { _ =>
      Position.generate
    }.get

  def partialFunction(): PartialFunction[Unit, Position] = {
    case _ => Position.generate
  }

  def recoverLambda(): Position =
    Try(throw new RuntimeException("")).recover { case NonFatal(_) => Position.generate }.get

  private class FakeClass {

    def method(): Position = Position.generate

    def lambda(): Position =
      Some("x").map(_ => Position.generate).get

    def forComprehension(): Position =
      (for {
        _ <- Some("x")
      } yield Position.generate).get

    def generatedPos(): Position =
      outerImplicitPos()

    def outerImplicitPos()(implicit pos: Position): Position = pos

    def nestedPos(): Position = {
      def nested1()(implicit pos: Position): Position = pos

      def nested2(): Position = nested1()

      nested2()
    }
  }

}
