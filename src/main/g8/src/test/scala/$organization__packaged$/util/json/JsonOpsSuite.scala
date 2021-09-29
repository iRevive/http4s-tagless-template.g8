package $organization$.util.json

import cats.data.NonEmptyList
import cats.mtl.Handle.handleKleisli
import cats.mtl.implicits.*
import $organization$.test.SimpleEffSuite
import $organization$.util.api.ErrorHandlerSuite.Eff
import $organization$.util.error.{ErrorChannel, ErrorIdGen}
import $organization$.util.error.AppError.select
import $organization$.util.syntax.json.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{DecodingFailure, Json}
import io.odin.meta.Render

object JsonOpsSuite extends SimpleEffSuite {

  import JsonOpsSuite.*

  test("decode. fail in case of missing or invalid fields") {
    val input = Json.obj(
      "field1" := 123,
      "field3" := 4321
    )

    for {
      resultT <- input.decodeF[Eff, JsonModel].attemptHandle
    } yield {
      val result = resultT.left.toOption.flatMap(_.error.select[JsonDecodingError])

      val expectedErrors = NonEmptyList.of(
        "DecodingFailure at .field1: String",
        "DecodingFailure at .field2: Attempt to decode value on failed cursor"
      )

      expect.all(
        result.map(_.json) == Some(input),
        result.map(_.targetClass) == Some("JsonModel"),
        result.map(_.errors.map(Render[DecodingFailure].render)) == Some(expectedErrors)
      )
    }
  }

  test("return a parsed model") {
    val input = Json.obj(
      "field1" := "test field",
      "field2" := 123,
      "field3" := 4321
    )

    val expected = JsonModel("test field", 123, 4321L)

    for {
      result <- input.decodeF[Eff, JsonModel]
    } yield expect(result == expected)
  }

  final case class JsonModel(field1: String, field2: Int, field3: Long)

  private implicit val errorChannel: ErrorChannel[Eff] =
    ErrorChannel.create(ErrorIdGen.const("test"))

}
