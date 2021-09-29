package $organization$.util

import cats.Applicative
import $organization$.persistence.postgres.PostgresError
import $organization$.service.user.UserValidationError
import $organization$.service.user.api.UserValidationErrorResponse
import $organization$.util.api.ErrorResponseSelector
import $organization$.util.json.JsonDecodingError
import $organization$.util.logging.RenderInstances
import io.odin.meta.Render
import io.github.irevive.union.derivation.UnionDerivation
import org.http4s.Response

import scala.reflect.TypeTest

package object error {

  type AppError = UserValidationError | PostgresError | JsonDecodingError

  object AppError {
    extension (error: AppError) {
      def select[E <: AppError](using TypeTest[AppError, E]): Option[E] =
        error match {
          case err: E => Some(err)
          case _      => None
        }
    }

    given Render[AppError]          = UnionDerivation.derive[Render, AppError]
    given ThrowableSelect[AppError] = UnionDerivation.derive[ThrowableSelect, AppError]

    implicit def appErrorResponse[F[_]: Applicative]: ErrorResponseSelector[F, AppError] =
      new ErrorResponseSelector[F, AppError] {
        def toResponse(value: AppError, errorId: String): Response[F] =
          value match {
            case error: UserValidationError => UserValidationErrorResponse.userValidationErrorResponse.toResponse(error, errorId)
            case error: PostgresError       => ErrorResponseSelector.postgresErrorResponse[F].toResponse(error, errorId)
            case error: JsonDecodingError   => ErrorResponseSelector.jsonDecodingErrorResponse[F].toResponse(error, errorId)
          }
      }
  }

}
