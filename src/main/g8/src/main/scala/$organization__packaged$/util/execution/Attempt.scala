package $organization$.util.execution

import cats.MonadError
import cats.mtl.ApplicativeHandle
import cats.mtl.syntax.raise._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.functor._

object Attempt {

  def attempt[F[_]: MonadError[?[_], Throwable], E: ApplicativeHandle[F, ?], A](fa: F[A]): F[Result[E, A]] =
    ApplicativeHandle[F, E]
      .attempt(fa)
      .attempt
      .map {
        case Left(unhandledError) => Result.UnhandledError(unhandledError)
        case Right(Left(error))   => Result.Error(error)
        case Right(Right(value))  => Result.Success(value)
      }

  def toEffect[F[_]: MonadError[?[_], Throwable], E: ApplicativeHandle[F, ?], A](result: Result[E, A]): F[A] =
    result match {
      case Attempt.Result.UnhandledError(unhandledError) => unhandledError.raiseError
      case Attempt.Result.Error(error)                   => error.raise
      case Attempt.Result.Success(value)                 => value.pure[F]
    }

  sealed trait Result[E, A]
  object Result {
    final case class Success[E, A](value: A)                extends Result[E, A]
    final case class Error[E, A](cause: E)                  extends Result[E, A]
    final case class UnhandledError[E, A](cause: Throwable) extends Result[E, A]
  }

}
