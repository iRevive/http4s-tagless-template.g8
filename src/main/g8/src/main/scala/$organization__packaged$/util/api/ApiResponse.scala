package $organization$.util.api

import io.circe.syntax.*
import io.circe.{Encoder, Json}

enum ApiResponse {
  case Success[A](result: A)
  case Error(error: String, errorId: String)
}

object ApiResponse {

  given [A: Encoder]: Encoder[Success[A]] =
    Encoder.instance(v => Json.obj("success" := true, "result" := v.result))

  given Encoder[Error] =
    Encoder.instance(v => Json.obj("success" := false, "error" := v.error, "errorId" := v.errorId))

}
