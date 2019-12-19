package $organization$.service.user.api

import cats.Applicative
import $organization$.service.user.UserValidationError
import $organization$.util.api.ErrorResponseSelector
import $organization$.util.syntax.logging._
import org.http4s.Status

object UserValidationErrorResponse {

  implicit def userValidationErrorResponse[F[_]: Applicative]: ErrorResponseSelector[F, UserValidationError] = {
    case (UserValidationError.UserNotFound(userId), errorId) =>
      ErrorResponseSelector.apiResponse[F](Status.NotFound, log"User with userId [\$userId] does not exist", errorId)
  }

}
