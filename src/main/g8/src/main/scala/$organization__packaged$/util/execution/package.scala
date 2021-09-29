package $organization$.util

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import $organization$.util.error.RaisedError
import $organization$.util.trace.LogContext

package object execution {

  type WithLogContext[F[_], A] = Kleisli[F, LogContext, A]
  type WithError[A]            = EitherT[IO, RaisedError, A]
  type Eff[A]                  = WithLogContext[WithError, A]

}
