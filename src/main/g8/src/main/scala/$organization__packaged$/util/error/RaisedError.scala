package $organization$.util.error

import $organization$.util.Position
import $organization$.util.logging.Loggable

@scalaz.deriving(Loggable)
final case class RaisedError(error: AppError, pos: Position, errorId: String) {

  def toException: RuntimeException =
    ThrowableExtractor[AppError].select(error) match {
      case Some(cause) => new RuntimeException(Loggable[RaisedError].show(this), cause)
      case None        => new RuntimeException(Loggable[RaisedError].show(this))
    }

}

object RaisedError {

  def withErrorId(error: AppError)(implicit pos: Position): RaisedError =
    RaisedError(error, pos, generateErrorId)

  def generateErrorId: String = scala.util.Random.alphanumeric.take(6).mkString

}
