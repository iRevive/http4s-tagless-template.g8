package $organization$.util.json

import cats.mtl.implicits._
import cats.mtl.Handle.handleKleisli
import cats.data.NonEmptyList
import $organization$.test.BaseSpec
import $organization$.util.syntax.json._
import io.circe.{DecodingFailure, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import io.odin.meta.Render

class JsonOpsSpec extends BaseSpec {

  import JsonOpsSpec._

  "JsonOps" when {

    "#decode" should {

      "fail in case of missing or invalid fields" in EffectAssertion() {
        val input = Json.obj(
          "field1" := 123,
          "field3" := 4321
        )

        for {
          resultT <- input.decodeF[Eff, JsonModel].attemptHandle
        } yield {
          val result = resultT.leftValue.error.select[JsonDecodingError].value

          val expectedErrors = NonEmptyList.of(
            "DecodingFailure at .field1: String",
            "DecodingFailure at .field2: Attempt to decode value on failed cursor"
          )

          result.json shouldBe input
          result.targetClass shouldBe "JsonModel"
          result.errors.map(Render[DecodingFailure].render) shouldBe expectedErrors
        }
      }

      "return a parsed model" in EffectAssertion() {
        val input = Json.obj(
          "field1" := "test field",
          "field2" := 123,
          "field3" := 4321
        )

        val expected = JsonModel("test field", 123, 4321L)

        for {
          result <- input.decodeF[Eff, JsonModel]
        } yield result shouldBe expected
      }

    }

  }

}

object JsonOpsSpec {

  final case class JsonModel(field1: String, field2: Int, field3: Long)

}
