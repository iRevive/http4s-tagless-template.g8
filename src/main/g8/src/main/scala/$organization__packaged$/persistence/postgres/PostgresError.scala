package $organization$.persistence.postgres

import $organization$.util.error.ThrowableSelect
import $organization$.util.instances.render._
import io.odin.meta.Render

@scalaz.annotation.deriving(Render)
sealed trait PostgresError

object PostgresError {

  final case class UnavailableConnection(cause: Throwable) extends PostgresError

  final case class ConnectionAttemptTimeout(message: String) extends PostgresError

  def unavailableConnection(cause: Throwable): PostgresError   = UnavailableConnection(cause)
  def connectionAttemptTimeout(message: String): PostgresError = ConnectionAttemptTimeout(message)

  implicit val postgresErrorThrowableSelect: ThrowableSelect[PostgresError] = {
    case _: ConnectionAttemptTimeout  => None
    case UnavailableConnection(cause) => Option(cause)
  }

}
