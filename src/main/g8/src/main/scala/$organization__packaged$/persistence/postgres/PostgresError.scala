package $organization$.persistence.postgres

import $organization$.util.error.ThrowableSelect
import $organization$.util.instances.render.*
import io.odin.extras.derivation.render.derived
import io.odin.meta.Render

enum PostgresError derives Render {
  case UnavailableConnection(cause: Throwable)
  case ConnectionAttemptTimeout(message: String)
}

object PostgresError {

  given postgresErrorThrowableSelect: ThrowableSelect[PostgresError] = {
    case _: ConnectionAttemptTimeout  => None
    case UnavailableConnection(cause) => Option(cause)
  }

}
