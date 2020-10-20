package $organization$.util

import cats.mtl.{Handle, Raise}
import $organization$.persistence.postgres.PostgresError
import $organization$.service.user.UserValidationError
import $organization$.util.config.ConfigParsingError
import $organization$.util.json.JsonDecodingError
import shapeless._

package object error {

  type AppError = UserValidationError :+: PostgresError :+: JsonDecodingError :+: ConfigParsingError :+: CNil

  type ErrorRaise[F[_]] = Raise[F, RaisedError]

  object ErrorRaise {
    def apply[F[_]](implicit instance: ErrorRaise[F]): ErrorRaise[F] = instance
  }

  type ErrorHandle[F[_]] = Handle[F, RaisedError]

  object ErrorHandle {
    def apply[F[_]](implicit instance: ErrorHandle[F]): ErrorHandle[F] = instance
  }

}
