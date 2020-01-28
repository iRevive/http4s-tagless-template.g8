package $organization$.util.error

import cats.Functor
import cats.syntax.functor._
import $organization$.util.Position
import $organization$.util.instances.render._
import io.odin.meta.{Render, ToThrowable}

@scalaz.deriving(Render)
final case class RaisedError(error: AppError, pos: Position, errorId: String) {

  def toException: RuntimeException =
    ThrowableSelect[AppError].select(error) match {
      case Some(cause) => new RuntimeException(Render[RaisedError].render(this), cause)
      case None        => new RuntimeException(Render[RaisedError].render(this))
    }

}

object RaisedError {

  def withErrorId[F[_]: Functor: ErrorIdGen](error: AppError)(implicit pos: Position): F[RaisedError] =
    for {
      id <- ErrorIdGen[F].gen
    } yield RaisedError(error, pos, id)

  implicit val raisedErrorToThrowable: ToThrowable[RaisedError] = _.toException

}
