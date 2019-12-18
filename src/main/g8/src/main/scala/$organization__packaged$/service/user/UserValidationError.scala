package $organization$.service.user

import $organization$.service.user.domain.UserId
import $organization$.util.error.ThrowableSelect
import $organization$.util.logging.Loggable

@scalaz.deriving(Loggable, ThrowableSelect.Empty)
sealed trait UserValidationError

object UserValidationError {

  final case class UserNotFound(userId: UserId) extends UserValidationError

  def userNotFound(userId: UserId): UserValidationError = UserNotFound(userId)

}
