package $organization$.util.api

import cats.Applicative
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.{Response, Status}
import shapeless._

trait ErrorResponseSelector[F[_], E] {
  def toResponse(value: E, errorId: String): Response[F]
}

object ErrorResponseSelector {
  def apply[F[_], E](implicit ev: ErrorResponseSelector[F, E]): ErrorResponseSelector[F, E] = ev

  def badRequestResponse[F[_]: Applicative, E](toErrorMessage: E => String): ErrorResponseSelector[F, E] =
    (e, errorId) => apiResponse(Status.BadRequest, toErrorMessage(e), errorId)

  def apiResponse[F[_]: Applicative](status: Status, message: String, errorId: String): Response[F] =
    Response[F](status).withEntity(ApiResponse.Error(message, errorId).asJson)

  implicit def cnilErrorResponseSelector[F[_]]: ErrorResponseSelector[F, CNil] =
    (value: CNil, _: String) => value.impossible

  implicit def coproductErrorSelector[F[_], H, T <: Coproduct](
      implicit h: Lazy[ErrorResponseSelector[F, H]],
      t: ErrorResponseSelector[F, T]
  ): ErrorResponseSelector[F, H :+: T] =
    (value, errorId) => value.eliminate(h.value.toResponse(_, errorId), t.toResponse(_, errorId))

}
