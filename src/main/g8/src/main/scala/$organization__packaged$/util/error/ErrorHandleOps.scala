package $organization$.util.error

import cats.MonadError

final class ErrorHandleOps[F[_], A](val fa: F[A]) extends AnyVal {

  def wrapUnhandled(implicit r: ErrorRaise[F], ma: MonadError[F, Throwable]): F[A] = ErrorHandle.wrapUnhandled[F](fa)

}

trait ToErrorHandleOps {

  final implicit def toErrorHandleOps[F[_], A](fa: F[A]): ErrorHandleOps[F, A] = new ErrorHandleOps(fa)

}
