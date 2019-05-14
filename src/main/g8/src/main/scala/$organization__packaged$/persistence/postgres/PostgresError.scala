package $organization$.persistence.postgres

import $organization$.util.Position
import $organization$.util.error.{BaseError, ThrowableError}

sealed trait PostgresError extends BaseError

object PostgresError {

  final case class UnhandledPostgresError(cause: Throwable)(implicit val pos: Position) extends PostgresError with ThrowableError

}
