package $organization$.util.config

import $organization$.test.BaseSpec
import $organization$.util.syntax.config._
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._

class ConfigOpsSpec extends BaseSpec {

  import ConfigOpsSpec._

  "ConfigOps" when {

    "#load" should {

      "load a valid config using generated scheme" in {
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

        config.load[ConfigModel]("service") should beRight(expected)
      }

      "return an error in case of invalid mapping" in {
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

        val result = config.load[ConfigModel]("service").leftValue

        result.path shouldBe "service"
        result.expectedClass shouldBe "ConfigModel"
        result.error.getMessage shouldBe "Attempt to decode value on failed cursor: DownField(stringField)"
      }

    }

    "#loadMeta" should {

      val tripleQuote = "\"\"\""

      "return an error" when {

        "config value is not a valid config" in {
          val input =
            """
              |{
              |   "some-property": \$\${test},
              |   "intField: 1
              |}
            """.stripMargin

          val config = ConfigFactory.parseString(s"service = \$tripleQuote\$input\$tripleQuote")

          val result = config.loadMeta[ConfigModel]("service").leftValue

          inside(result) {
            case ConfigParsingError(path, expectedClass, error) =>
              path shouldBe "service"
              expectedClass shouldBe "ConfigModel"
              error.getMessage shouldBe s"Couldn't parse [\$input] as config"
          }
        }

        "config value has invalid format" in {
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

          val result = config.loadMeta[ConfigModel]("service").leftValue

          inside(result) {
            case ConfigParsingError(path, expectedClass, error) =>
              path shouldBe "service"
              expectedClass shouldBe "ConfigModel"
              error.getMessage shouldBe "Attempt to decode value on failed cursor: DownField(stringField)"
          }
        }

      }

      "load a valid config" in {
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

        config.loadMeta[ConfigModel]("service") should beRight(expected)
      }

    }
  }

}

object ConfigOpsSpec {

  final case class ConfigModel(stringField: String, intField: Int)

}
