package $organization$.util.error

import cats.effect.Sync
import cats.syntax.functor._
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

  def withErrorId[F[_]: Sync](error: AppError)(implicit pos: Position): F[RaisedError] =
    for {
      id <- generateErrorId[F]
    } yield RaisedError(error, pos, id)

  def generateErrorId[F[_]: Sync]: F[String] = Sync[F].delay(scala.util.Random.alphanumeric.take(6).mkString)

}
