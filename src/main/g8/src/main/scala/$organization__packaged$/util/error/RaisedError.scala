package $organization$.util.error

import cats.Functor
import cats.syntax.functor.*
import cats.syntax.contravariant.*
import $organization$.util.error.RaisedError.RaisedException
import $organization$.util.instances.render.*
import io.odin.extras.derivation.render.*
import io.odin.meta.{Position, Render, ToThrowable}

final case class RaisedError(error: AppError, pos: Position, errorId: String) derives Render {
  def toException: RaisedException = RaisedException(this)
}

object RaisedError {
  import AppError.given

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  final case class RaisedException(error: RaisedError)
      extends RuntimeException(
        Render[RaisedError].render(error),
        ThrowableSelect[AppError].select(error.error).orNull
      )

  def withErrorId[F[_]](error: AppError)(using F: Functor[F], gen: ErrorIdGen[F], pos: Position): F[RaisedError] =
    for {
      id <- ErrorIdGen[F].gen
    } yield RaisedError(error, pos, id)

  given ToThrowable[RaisedError]     = _.toException
  given Render[Position]             = p => s"\${p.enclosureName}:\${p.line.toString}"
  given ThrowableSelect[RaisedError] = ThrowableSelect[AppError].contramap(_.error)

}
