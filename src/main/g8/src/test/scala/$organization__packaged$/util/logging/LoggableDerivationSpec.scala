package $organization$.util.logging

import $organization$.test.BaseSpec

class LoggableDerivationSpec extends BaseSpec {

  import LoggableDerivationSpec._

  "LoggableDerivation" should {

    "derive typeclass according to the structure of a class" in {
      val loggableInstance = LoggableDerivation.derive[TestClass]

      val obj = TestClass(
        arg1 = Some("arg 1 string"),
        arg2 = None,
        arg3 = Some(10.21),
        arg4 = Nil,
        arg5 = List(321L)
      )

      val result = loggableInstance.show(obj)

      val expected = "TestClass(arg1 = arg 1 string, arg3 = 10.21, arg5 = [321])"

      result shouldBe expected
    }

    "derive typeclass for a value class" in {
      val loggableInstance = LoggableDerivation.derive[ValueClassTest]

      val obj = ValueClassTest("arg 1 string")

      val result = loggableInstance.show(obj)

      val expected = "arg 1 string"

      result shouldBe expected
    }

    "derive typeclass for a sealed trait" in {
      val loggableInstance = LoggableDerivation.derive[TraitType]

      val obj1 = TraitType1("arg 1 string")
      val obj2 = TraitType2(1)

      loggableInstance.show(obj1) shouldBe "TraitType1(arg1 = arg 1 string)"
      loggableInstance.show(obj2) shouldBe "TraitType2(arg1 = 1)"
    }

  }

}

object LoggableDerivationSpec {

  case class TestClass(
      arg1: Option[String],
      arg2: Option[Double],
      arg3: Option[Double],
      arg4: List[Long],
      arg5: List[Long]
  )

  case class ValueClassTest(arg: String) extends AnyVal

  sealed trait TraitType

  case class TraitType1(arg1: String) extends TraitType

  case class TraitType2(arg1: Int) extends TraitType

}
