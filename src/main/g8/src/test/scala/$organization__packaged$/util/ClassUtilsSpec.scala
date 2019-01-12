package $organization$.util

import org.scalatest.{Matchers, WordSpec}

class ClassUtilsSpec extends WordSpec with Matchers {

  "ClassUtils" should {

    "correctly extract a name of nested class" in {
      val clazz = new ClassUtilsSpec.services.TestClass

      val error = intercept[java.lang.InternalError](clazz.getClass.getSimpleName)
      error.getMessage shouldBe "Malformed class name"

      ClassUtils.getClassSimpleName(clazz.getClass) shouldBe "TestClass"
    }

  }

}

object ClassUtilsSpec {

  private[ClassUtilsSpec] object services {

    class TestClass()

  }

}
