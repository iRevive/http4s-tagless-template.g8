package $organization$.util.error

import $organization$.persistence.mongo.MongoError
import $organization$.persistence.postgres.PostgresError
import $organization$.util.Position
import $organization$.util.logging.Loggable
import shapeless.Coproduct

@scalaz.deriving(Loggable)
final case class RaisedError(error: AppError, pos: Position) {

  def toException: RuntimeException =
    error.fold(AppError.getException) match {
      case Some(cause) => new RuntimeException(Loggable[RaisedError].show(this), cause)
      case None        => new RuntimeException(Loggable[RaisedError].show(this))
    }

}

object RaisedError {

  def postgres[E <: PostgresError](e: E)(implicit pos: Position): RaisedError =
    RaisedError(Coproduct[AppError](e: PostgresError), pos)

  def mongo[E <: MongoError](e: E)(implicit pos: Position): RaisedError =
    RaisedError(Coproduct[AppError](e: MongoError), pos)

}
