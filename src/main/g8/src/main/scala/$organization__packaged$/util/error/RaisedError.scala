package $organization$.util.error

import cats.Functor
import cats.syntax.functor._
import $organization$.util.Position
import $organization$.util.logging.Loggable

@scalaz.annotation.deriving(Loggable)
final case class RaisedError(error: AppError, pos: Position, errorId: String) {

  def toException: RuntimeException =
    ThrowableSelect[AppError].select(error) match {
      case Some(cause) => new RuntimeException(Loggable[RaisedError].show(this), cause)
      case None        => new RuntimeException(Loggable[RaisedError].show(this))
    }

}

object RaisedError {

  def withErrorId[F[_]: Functor: ErrorIdGen](error: AppError)(implicit pos: Position): F[RaisedError] =
    for {
      id <- ErrorIdGen[F].gen
    } yield RaisedError(error, pos, id)

}
