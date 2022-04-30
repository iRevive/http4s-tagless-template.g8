package $organization$.util.api

import cats.Applicative
import $organization$.persistence.postgres.PostgresError
import $organization$.util.json.JsonDecodingError
import io.circe.syntax.*
import io.odin.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Response, Status}

trait ErrorResponseSelector[F[_], E] {
  def toResponse(value: E, errorId: String): Response[F]
}

object ErrorResponseSelector extends ErrorResponseSelectorInstances {

  def apply[F[_], E](implicit ev: ErrorResponseSelector[F, E]): ErrorResponseSelector[F, E] = ev

  def badRequestResponse[F[_]: Applicative, E](toErrorMessage: E => String): ErrorResponseSelector[F, E] =
    (e, errorId) => apiResponse(Status.BadRequest, toErrorMessage(e), errorId)

  def apiResponse[F[_]: Applicative](status: Status, message: String, errorId: String): Response[F] =
    Response[F](status).withEntity(ApiResponse.Error(message, errorId).asJson)

}

trait ErrorResponseSelectorInstances {

  implicit def jsonDecodingErrorResponse[F[_]: Applicative]: ErrorResponseSelector[F, JsonDecodingError] =
    ErrorResponseSelector.badRequestResponse(e => render"Json decoding error. \${e.errors}")

  implicit def postgresErrorResponse[F[_]: Applicative]: ErrorResponseSelector[F, PostgresError] =
    ErrorResponseSelector.badRequestResponse {
      case PostgresError.UnavailableConnection(_)    => "Postgres connection is not available"
      case PostgresError.ConnectionAttemptTimeout(_) => "Postgres connection timeout"
    }

}
