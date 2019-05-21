package $organization$.util.error

import $organization$.persistence.mongo.MongoError
import $organization$.persistence.postgres.PostgresError
import $organization$.util.config.ConfigParsingError
import $organization$.util.json.JsonParsingError
import shapeless.Poly1

private[error] trait AppErrorOps {

  object getException extends Poly1 {
    private def noException[A]: Case.Aux[A, Option[Throwable]] = at[A](_ => None)

    implicit def caseConfigParsingError: Case.Aux[ConfigParsingError, Option[Throwable]] = noException
    implicit def caseJsonParsingError: Case.Aux[JsonParsingError, Option[Throwable]]     = noException

    implicit def caseMongoError: Case.Aux[MongoError, Option[Throwable]] =
      at[MongoError] {
        case _: MongoError.ConnectionAttemptTimeout  => None
        case MongoError.UnavailableConnection(cause) => Some(cause)
        case MongoError.ExecutionError(cause)        => Some(cause)
      }

    implicit def casePostgresError: Case.Aux[PostgresError, Option[Throwable]] =
      at[PostgresError] {
        case _: PostgresError.ConnectionAttemptTimeout  => None
        case PostgresError.UnavailableConnection(cause) => Some(cause)
      }
  }

}
