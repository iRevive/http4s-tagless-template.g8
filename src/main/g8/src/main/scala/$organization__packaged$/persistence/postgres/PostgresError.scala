package $organization$.persistence.postgres

import $organization$.util.logging.Loggable

@scalaz.deriving(Loggable)
sealed trait PostgresError

object PostgresError {

  final case class UnavailableConnection(cause: Throwable) extends PostgresError

  final case class ConnectionAttemptTimeout(message: String) extends PostgresError

}
