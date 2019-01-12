package $organization$.util

import cats.Applicative
import cats.mtl.{ApplicativeHandle, FunctorRaise}

package object error {

  type ErrorRaise[F[_]] = FunctorRaise[F, BaseError]

  object ErrorRaise {

    def apply[F[_]](implicit instance: ErrorRaise[F]): ErrorRaise[F] = instance

    def fromEither[F[_]]: FromEitherPartiallyApplied[F] = new FromEitherPartiallyApplied[F]

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    final class FromEitherPartiallyApplied[F[_]](private val dummy: Boolean = true) extends AnyVal {
      def apply[E <: BaseError, A](value: Either[E, A])(implicit F: Applicative[F], raise: ErrorRaise[F]): F[A] =
        value.fold[F[A]](raise.raise, F.pure)
    }

  }

  type ErrorHandle[F[_]] = ApplicativeHandle[F, BaseError]

  object ErrorHandle {

    def apply[F[_]](implicit instance: ErrorHandle[F]): ErrorHandle[F] = instance

  }

}
