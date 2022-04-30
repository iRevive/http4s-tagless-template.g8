package $organization$.service.user

import $organization$.service.user.domain.UserId
import $organization$.util.error.ThrowableSelect
import io.odin.extras.derivation.render.derived
import io.odin.meta.Render

enum UserValidationError derives Render, ThrowableSelect.Empty {
  case UserNotFound(userId: UserId)
}
