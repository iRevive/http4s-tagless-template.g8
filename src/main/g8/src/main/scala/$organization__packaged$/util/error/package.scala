package $organization$.util

import cats.MonadError
import cats.mtl.{ApplicativeHandle, FunctorRaise}
import cats.syntax.applicativeError._

package object error {

  type ErrorRaise[F[_]] = FunctorRaise[F, BaseError]

  object ErrorRaise {

    def apply[F[_]](implicit instance: ErrorRaise[F]): ErrorRaise[F] = instance

  }

  type ErrorHandle[F[_]] = ApplicativeHandle[F, BaseError]

  object ErrorHandle {

    def apply[F[_]](implicit instance: ErrorHandle[F]): ErrorHandle[F] = instance

    def wrapUnhandled[F[_]]: WrapUnhandledPartiallyApplied[F] = new WrapUnhandledPartiallyApplied[F]

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    final class WrapUnhandledPartiallyApplied[F[_]](private val dummy: Boolean = true) extends AnyVal {
      def apply[A](flow: F[A])(implicit F: MonadError[F, Throwable], raise: ErrorRaise[F]): F[A] =
        flow.handleErrorWith(e => raise.raise(ThrowableError(e)))
    }

  }

}
