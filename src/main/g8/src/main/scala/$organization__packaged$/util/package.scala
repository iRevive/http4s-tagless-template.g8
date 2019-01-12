package $organization$

import cats.data.{EitherT, Kleisli}
import $organization$.util.error.BaseError
import $organization$.util.logging.TraceId

package object util {

  type Traced[F[_], A] = Kleisli[F, TraceId, A]

  type TracedResultT[F[_], A] = Traced[EitherT[F, BaseError, ?], A]

}
