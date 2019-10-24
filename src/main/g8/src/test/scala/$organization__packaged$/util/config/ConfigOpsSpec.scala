package $organization$.util.config

import cats.mtl.implicits._
import $organization$.test.BaseSpec
import $organization$.util.syntax.config._
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._

class ConfigOpsSpec extends BaseSpec {

  import ConfigOpsSpec._

  "ConfigOps" when {

    "#loadF" should {

      "load a valid config using generated scheme" in EffectAssertion() {
        val config = ConfigFactory.parseString(
          """
            |service {
            |
            |   stringField = "some message"
            |   intField = 1
            |
            |}
          """.stripMargin
        )

        val expected = ConfigModel("some message", 1)

        for {
          cfg <- config.loadF[Eff, ConfigModel]("service")
        } yield cfg shouldBe expected
      }

      "return an error in case of invalid mapping" in EffectAssertion() {
        val config = ConfigFactory.parseString(
          """
            |service {
            |
            |   some-property = "message"
            |   intField = 1
            |
            |}
          """.stripMargin
        )

        for {
          resultT <- config.loadF[Eff, ConfigModel]("service").attemptHandle
        } yield {
          val result = resultT.leftValue.error.select[ConfigParsingError].value

          result.path shouldBe "service"
          result.expectedClass shouldBe "ConfigModel"
          result.error.getMessage shouldBe "Attempt to decode value on failed cursor: DownField(stringField)"
        }
      }

    }

    "#loadMetaF" should {

      val tripleQuote = "\"\"\""

      "return an error" when {

        "config value is not a valid config" in EffectAssertion() {
          val input =
            """
              |{
              |   "some-property": \$\${test},
              |   "intField: 1
              |}
            """.stripMargin

          val config = ConfigFactory.parseString(s"service = \$tripleQuote\$input\$tripleQuote")

          for {
            resultT <- config.loadMetaF[Eff, ConfigModel]("service").attemptHandle
          } yield {
            val result = resultT.leftValue.error.select[ConfigParsingError].value

            result.path shouldBe "service"
            result.expectedClass shouldBe "ConfigModel"
            result.error.getMessage shouldBe s"Couldn't parse [\$input] as config"
          }
        }

        "config value has invalid format" in EffectAssertion() {
          val config = ConfigFactory.parseString(
            s"""
              |service = \$tripleQuote{
              |
              |   some-property = "message"
              |   intField = 1
              |
              |}\$tripleQuote
            """.stripMargin
          )

          for {
            resultT <- config.loadMetaF[Eff, ConfigModel]("service").attemptHandle
          } yield {
            val result = resultT.leftValue.error.select[ConfigParsingError].value

            result.path shouldBe "service"
            result.expectedClass shouldBe "ConfigModel"
            result.error.getMessage shouldBe "Attempt to decode value on failed cursor: DownField(stringField)"
          }
        }

      }

      "load a valid config" in EffectAssertion() {
        val config = ConfigFactory.parseString(
          s"""
            |service = \$tripleQuote{
            |
            |   stringField = "some message"
            |   intField = 1
            |
            |}\$tripleQuote
          """.stripMargin
        )

        val expected = ConfigModel("some message", 1)

        for {
          result <- config.loadMetaF[Eff, ConfigModel]("service")
        } yield result shouldBe expected
      }

    }

  }

}

object ConfigOpsSpec {

  final case class ConfigModel(stringField: String, intField: Int)

}
