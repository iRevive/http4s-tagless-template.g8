package $organization$.util.json

import cats.data.NonEmptyList
import $organization$.test.BaseSpec
import $organization$.util.json.JsonOpsSpec.JsonModel
import $organization$.util.logging.Loggable
import $organization$.util.syntax.json._
import io.circe.{DecodingFailure, Json}
import io.circe.generic.auto._
import io.circe.syntax._

class JsonOpsSpec extends BaseSpec {

  import JsonOpsSpec._

  "JsonOps" when {

    "#decode" should {

      "fail in case of missing or invalid fields" in {
        val input = Json.obj(
          "field1" := 123,
          "field3" := 4321
        )

        inside(input.decode[JsonModel].leftValue) {
          case JsonDecodingError(json, className, errors) =>
            val expectedErrors = NonEmptyList.of(
              "DecodingFailure at .field1: String",
              "DecodingFailure at .field2: Attempt to decode value on failed cursor"
            )

            json shouldBe input
            className shouldBe "JsonModel"
            errors.map(Loggable[DecodingFailure].show) shouldBe expectedErrors
        }
      }

      "return a parsed model" in {
        val input = Json.obj(
          "field1" := "test field",
          "field2" := 123,
          "field3" := 4321
        )

        val result = input.decode[JsonModel].value

        val expected = JsonModel("test field", 123, 4321L)

        result shouldBe expected
      }

    }

  }

}

object JsonOpsSpec {

  final case class JsonModel(field1: String, field2: Int, field3: Long)

}
