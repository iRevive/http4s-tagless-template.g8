package $organization$.util

import cats.effect.Timer
import cats.mtl.ApplicativeHandle
import cats.mtl.syntax.raise._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.{Applicative, MonadError}
import $organization$.util.logging.{Loggable, TracedLogger}
import $organization$.util.syntax.logging._
import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.Decoder
import io.circe.config.syntax.durationDecoder
import io.circe.refined._

import scala.concurrent.duration.FiniteDuration

object Retry {

  def retry[F[_]: MonadError[?[_], Throwable]: Timer, E: ApplicativeHandle[F, ?], A](
      fa: F[A],
      policy: Policy,
      decider: Decider[E, A],
      logger: Logger[F, E, A]
  ): F[A] = {

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def loop(retries: Int): F[A] =
      ApplicativeHandle[F, E]
        .attempt(fa)
        .attempt
        .flatMap { input =>
          val retriesLeft = retries - 1

          val result: Result[E, A] = input match {
            case Left(unhandledError) => Result.UnhandledError(unhandledError)
            case Right(Left(error))   => Result.Error(error)
            case Right(Right(value))  => Result.Success(value)
          }

          val operation = decider.decide(result, retriesLeft)

          val next: F[A] = operation match {
            case Operation.Retry =>
              Timer[F].sleep(policy.delay) *> loop(retriesLeft)

            case Operation.Result | Operation.Rethrow =>
              input match {
                case Left(unhandledError) => unhandledError.raiseError
                case Right(Left(error))   => error.raise
                case Right(Right(value))  => value.pure[F]
              }
          }

          logger.log(result, retriesLeft, operation) *> next
        }

    loop(policy.retries.value)
  }

  @scalaz.deriving(Decoder, Loggable)
  final case class Policy(retries: NonNegInt, delay: FiniteDuration, timeout: FiniteDuration)

  trait Decider[E, A] {
    def decide(result: Result[E, A], retriesLeft: Int): Operation
  }

  object Decider {
    def default[E, A]: Retry.Decider[E, A] = { (result, retries) =>
      result match {
        case Result.Success(_) =>
          Retry.Operation.Result

        case Result.Error(_) =>
          if (retries > 0) Retry.Operation.Retry else Retry.Operation.Rethrow

        case Result.UnhandledError(_) =>
          if (retries > 0) Retry.Operation.Retry else Retry.Operation.Rethrow
      }
    }
  }

  trait Logger[F[_], E, A] {
    def log(result: Result[E, A], retries: Int, next: Operation): F[Unit]
  }

  object Logger {
    def noop[F[_]: Applicative, E, A]: Logger[F, E, A] = (_, _, _) => Applicative[F].unit

    def default[F[_]: Applicative, E: Loggable, A](logger: TracedLogger[F]): Logger[F, E, A] =
      (result, retries, next) => {
        result match {
          case Result.Success(_) =>
            logger.error(log"Retry policy. Success. Retires left [\$retries]. Next \$next")

          case Result.UnhandledError(cause) =>
            logger.error(log"Retry policy. Unhandled error \$cause. Retires left [\$retries]. Next \$next")

          case Result.Error(error) =>
            logger.error(log"Retry policy. Error \$error. Retires left [\$retries]. Next \$next")
        }
      }
  }

  sealed trait Result[E, A]
  object Result {
    final case class Success[E, A](value: A)                extends Result[E, A]
    final case class Error[E, A](cause: E)                  extends Result[E, A]
    final case class UnhandledError[E, A](cause: Throwable) extends Result[E, A]
  }

  @scalaz.deriving(Loggable)
  sealed trait Operation
  object Operation {
    final case object Result  extends Operation
    final case object Retry   extends Operation
    final case object Rethrow extends Operation
  }

}

trait ToRetryOps {
  final implicit def toRetryOps[F[_], A](fa: F[A]): RetryOps[F, A] = new RetryOps[F, A](fa)
}

final class RetryOps[F[_], A](private val fa: F[A]) extends AnyVal {

  def retryDefault[E: Loggable](
      policy: Retry.Policy,
      logger: TracedLogger[F]
  )(implicit F: MonadError[F, Throwable], T: Timer[F], E: ApplicativeHandle[F, E]): F[A] =
    Retry.retry(
      fa = fa,
      policy = policy,
      decider = Retry.Decider.default[E, A],
      logger = Retry.Logger.default[F, E, A](logger)
    )

  def retry[E](
      policy: Retry.Policy,
      decider: Retry.Decider[E, A],
      logger: Retry.Logger[F, E, A]
  )(implicit F: MonadError[F, Throwable], T: Timer[F], E: ApplicativeHandle[F, E]): F[A] =
    Retry.retry[F, E, A](fa, policy, decider, logger)

}
