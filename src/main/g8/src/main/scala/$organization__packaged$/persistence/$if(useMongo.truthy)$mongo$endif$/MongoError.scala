package $organization$.persistence.mongo

import $organization$.util.Position
import $organization$.util.error.{BaseError, ThrowableError}

sealed trait MongoError extends BaseError

object MongoError {

  final case class UnhandledMongoError(cause: Throwable)(implicit val pos: Position) extends MongoError with ThrowableError

}
