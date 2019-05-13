package $organization$.util.error

import cats.Applicative

final class ErrorRaiseOps[E <: BaseError, A](val fa: Either[E, A]) extends AnyVal {

  def pureOrRaise[F[_]](implicit E: ErrorRaise[F], A: Applicative[F]): F[A] = fa.fold[F[A]](E.raise, A.pure)

}

trait ToErrorRaiseOps {

  final implicit def toErrorRaiseOps[E <: BaseError, A](fa: Either[E, A]): ErrorRaiseOps[E, A] = new ErrorRaiseOps(fa)

}
