package $organization$.util.error

import cats.MonadThrow
import cats.effect.Temporal
import cats.mtl.Handle
import cats.syntax.flatMap.*
import io.odin.meta.Position
import retry.{RetryDetails, RetryPolicy}

trait ErrorChannel[F[_]] {
  given handle: Handle[F, RaisedError]
  given errorIdGen: ErrorIdGen[F]
  given monadThrow: MonadThrow[F]

  def raiseEither[E <: AppError, A](error: Either[E, A])(using Position): F[A] =
    error.fold(error => raise(error), value => monadThrow.pure(value))

  def raise[E <: AppError, A](error: E)(using Position): F[A] =
    RaisedError.withErrorId[F](error).flatMap(handle.raise)

  def retryMtl[A](fa: F[A], policy: RetryPolicy[F], onError: (RaisedError, RetryDetails) => F[Unit])(using Temporal[F]): F[A] =
    retry.mtl.retryingOnAllErrors(policy, onError)(fa)

}

object ErrorChannel {

  def apply[F[_]](using ev: ErrorChannel[F]): ErrorChannel[F] = ev

  def create[F[_]](idGen: ErrorIdGen[F])(using H: Handle[F, RaisedError], M: MonadThrow[F]): ErrorChannel[F] =
    new ErrorChannel[F] {
      def handle: Handle[F, RaisedError] = H
      def errorIdGen: ErrorIdGen[F]      = idGen
      def monadThrow: MonadThrow[F]      = M
    }

}
