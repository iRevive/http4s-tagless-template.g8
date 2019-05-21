package $organization$.util

import cats.mtl.{ApplicativeHandle, FunctorRaise}
import $organization$.persistence.mongo.MongoError
import $organization$.persistence.postgres.PostgresError
import $organization$.util.config.ConfigParsingError
import $organization$.util.json.JsonParsingError
import shapeless._

package object error {

  type AppError = MongoError :+: PostgresError :+: JsonParsingError :+: ConfigParsingError :+: CNil

  object AppError extends AppErrorOps

  type ErrorRaise[F[_]] = FunctorRaise[F, RaisedError]

  object ErrorRaise {
    def apply[F[_]](implicit instance: ErrorRaise[F]): ErrorRaise[F] = instance
  }

  type ErrorHandle[F[_]] = ApplicativeHandle[F, RaisedError]

  object ErrorHandle {
    def apply[F[_]](implicit instance: ErrorHandle[F]): ErrorHandle[F] = instance
  }

}
