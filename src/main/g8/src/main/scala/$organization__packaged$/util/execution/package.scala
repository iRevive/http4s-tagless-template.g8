package $organization$.util

import cats.data.{EitherT, Kleisli}
import $organization$.util.error.RaisedError
import $organization$.util.logging.TraceId
import monix.eval.Task

package object execution {

  type Traced[F[_], A] = Kleisli[F, TraceId, A]
  type Eff[A]          = Traced[EitherT[Task, RaisedError, *], A]

}
