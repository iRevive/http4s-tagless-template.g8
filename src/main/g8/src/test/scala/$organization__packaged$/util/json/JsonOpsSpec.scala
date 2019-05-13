package $organization$.util.json

import java.nio.charset.StandardCharsets

import $organization$.test.BaseSpec
import $organization$.util.json.JsonParsingError._
import $organization$.util.syntax.logging._
import io.circe.Json
import io.circe.generic.auto._

class JsonOpsSpec extends BaseSpec {

  import JsonOpsSpec._

  "JsonOps" when {

    "#parseJson" should {

      "fail in case of incorrect json" in {
        // missing closing bracket
        val input =
          """{"field": null"""

        inside(JsonOps.parseJson(input).leftValue) {
          case error @ NonParsableJson(receivedInput, _) =>
            val expectedMessage = log"Could not parse json from input [\$input]. Error: [exhausted input]"

            receivedInput shouldBe input
            error.message shouldBe expectedMessage
        }
      }

      "return a parsed json" in {
        import io.circe.syntax._

        val message = """{"field": 123}"""

        val result = JsonOps.parseJson(message).value

        result shouldBe Json.obj("field" := 123)
      }

    }

    "#decode" should {

      "fail in case of incorrect json" in {
        // missing closing bracket
        val input =
          """{"field": null"""

        val bytes = input.getBytes(StandardCharsets.UTF_8)

        inside(JsonOps.decode[JsonModel](bytes).leftValue) {
          case error @ NonParsableJson(receivedInput, _) =>
            val expectedMessage = log"Could not parse json from input [\$input]. Error: [exhausted input]"

            receivedInput shouldBe input
            error.message shouldBe expectedMessage
        }
      }

      "fail in case of missing or invalid fields" in {
        val input =
          """
            |{
            |  "field1": 123,
            |  "field3": 4321
            |}
          """.stripMargin

        val bytes = input.getBytes(StandardCharsets.UTF_8)

        inside(JsonOps.decode[JsonModel](bytes).leftValue) {
          case error @ JsonDecodingError(json, className, _) =>
            val expectedJson = io.circe.parser.parse(input).value

            val expectedMessage = s"Could not parse json as [JsonModel] from input [\${expectedJson.noSpaces}]. " +
              "Errors: [String: DownField(field1), Attempt to decode value on failed cursor: DownField(field2)]"

            expectedJson shouldBe json
            className shouldBe "JsonModel"
            error.message shouldBe expectedMessage
        }
      }

      "return a parsed model" in {
        val message =
          """
            |{
            |  "field1": "test field",
            |  "field2": 123,
            |  "field3": 4321
            |}
          """.stripMargin

        val result = JsonOps.decode[JsonModel](message.getBytes(StandardCharsets.UTF_8)).value

        val expected = JsonModel("test field", 123, 4321L)

        result shouldBe expected
      }

    }

  }

}

object JsonOpsSpec {

  case class JsonModel(field1: String, field2: Int, field3: Long)

}
