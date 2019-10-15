package $organization$.persistence.mongo

import $organization$.util.logging.Loggable
import $organization$.util.error.ThrowableExtractor

@scalaz.annotation.deriving(Loggable)
sealed trait MongoError

object MongoError {

  final case class UnavailableConnection(cause: Throwable) extends MongoError

  final case class ConnectionAttemptTimeout(message: String) extends MongoError

  final case class ExecutionError(cause: Throwable) extends MongoError

  def unavailableConnection(cause: Throwable): MongoError   = UnavailableConnection(cause)
  def connectionAttemptTimeout(message: String): MongoError = ConnectionAttemptTimeout(message)
  def executionError(cause: Throwable): MongoError          = ExecutionError(cause)

  implicit val mongoErrorThrowableExtractor: ThrowableExtractor[MongoError] = {
    case _: ConnectionAttemptTimeout  => None
    case UnavailableConnection(cause) => Option(cause)
    case ExecutionError(cause)        => Option(cause)
  }

}
