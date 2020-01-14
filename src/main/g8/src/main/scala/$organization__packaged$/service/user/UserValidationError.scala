package $organization$.service.user

import $organization$.service.user.domain.UserId
import $organization$.util.error.ThrowableSelect
import io.odin.meta.Render

@scalaz.deriving(Render, ThrowableSelect.Empty)
sealed trait UserValidationError

object UserValidationError {

  final case class UserNotFound(userId: UserId) extends UserValidationError

  def userNotFound(userId: UserId): UserValidationError = UserNotFound(userId)

}
