package $organization$.util
package error

import cats.Monad
import cats.syntax.flatMap._
import shapeless.ops.coproduct.Inject

trait ToErrorRaiseOps {
  final implicit def toErrorRaiseOps[E, A](fa: Either[E, A]): ErrorRaiseOps[E, A] = new ErrorRaiseOps(fa)
}

final class ErrorRaiseOps[E, A](val fa: Either[E, A]) extends AnyVal {

  def pureOrRaise[F[_]](implicit E: ErrorRaise[F], G: ErrorIdGen[F], F: Monad[F], I: Inject[AppError, E], P: Position): F[A] =
    fa.fold[F[A]](e => RaisedError.withErrorId[F](I.apply(e)).flatMap(E.raise), F.pure)

}
