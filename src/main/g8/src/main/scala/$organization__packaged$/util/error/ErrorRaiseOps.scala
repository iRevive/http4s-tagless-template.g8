package $organization$.util
package error

import cats.Applicative
import shapeless.ops.coproduct.Inject

trait ToErrorRaiseOps {
  final implicit def toErrorRaiseOps[E, A](fa: Either[E, A]): ErrorRaiseOps[E, A] = new ErrorRaiseOps(fa)
}

final class ErrorRaiseOps[E, A](val fa: Either[E, A]) extends AnyVal {

  def pureOrRaise[F[_]](implicit E: ErrorRaise[F], A: Applicative[F], I: Inject[AppError, E], P: Position): F[A] =
    fa.fold[F[A]](e => E.raise(RaisedError.withErrorId(I.apply(e))), A.pure)

}
