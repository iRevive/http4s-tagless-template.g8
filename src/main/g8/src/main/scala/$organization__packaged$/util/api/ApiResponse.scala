package $organization$.util.api

import io.circe.syntax.*
import io.circe.{Encoder, Json}

sealed trait ApiResponse

object ApiResponse {

  final case class Success[A](result: A) extends ApiResponse

  final case class Error(error: String, errorId: String) extends ApiResponse

  implicit def successResponseEncoder[A: Encoder]: Encoder[Success[A]] =
    Encoder.instance(v => Json.obj("success" := true, "result" := v.result))

  implicit val errorResponseEncoder: Encoder[Error] =
    Encoder.instance(v => Json.obj("success" := false, "error" := v.error, "errorId" := v.errorId))

}
