package $organization$.persistence.mongo

import $organization$.util.logging.Loggable

@scalaz.deriving(Loggable)
sealed trait MongoError

object MongoError {

  final case class UnavailableConnection(cause: Throwable) extends MongoError

  final case class ConnectionAttemptTimeout(message: String) extends MongoError

  final case class ExecutionError(cause: Throwable) extends MongoError

}
