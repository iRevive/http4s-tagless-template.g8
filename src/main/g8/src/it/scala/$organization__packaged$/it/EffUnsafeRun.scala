package $organization$.it

import cats.data.{EitherT, Kleisli, Nested}
import cats.effect.syntax.spawn.*
import cats.effect.unsafe.IORuntime
import cats.effect.{Async, Fiber, IO}
import cats.{Applicative, Monad, Parallel, ~>}
import $organization$.util.error.RaisedError
import $organization$.util.execution.{Eff, WithError}
import $organization$.util.trace.{LogContext, TraceId}
import weaver.UnsafeRun

class EffUnsafeRun(prefix: String) extends UnsafeRun[Eff] {

  type CancelToken = Fiber[Eff, Throwable, Unit]

  private implicit val runtime: IORuntime = IORuntime.global

  override implicit val parallel: Parallel[Eff] =
    new Parallel[Eff] {
      type F[A] = Nested[IO.Par, Either[RaisedError, *], A]

      private val inner: Parallel.Aux[WithError, Nested[IO.Par, Either[RaisedError, *], *]] = Parallel[WithError]

      def applicative: Applicative[F] = inner.applicative
      def monad: Monad[Eff]           = implicitly

      def sequential: F ~> Eff = inner.sequential.andThen(Kleisli.liftK)
      def parallel: Eff ~> F =
        new (Eff ~> F) {
          def apply[A](fa: Eff[A]): F[A] = inner.parallel(gen.flatMap(fa.run))
        }
    }

  override implicit val effect: Async[Eff] = Async.asyncForKleisli

  def cancel(token: CancelToken): Unit = sync(token.cancel)

  def background(task: Eff[Unit]): CancelToken = toIO(task.start).unsafeRunSync()

  def sync(task: Eff[Unit]): Unit = toIO(task).unsafeRunSync()

  def async(task: Eff[Unit]): Unit = toIO(task).unsafeRunAndForget()

  def toIO: Eff ~> IO =
    new (Eff ~> IO) {
      def apply[A](fa: Eff[A]): IO[A] =
        gen.flatMap(fa.run).leftSemiflatMap(e => IO.raiseError(e.toException)).merge
    }

  private val gen: EitherT[IO, RaisedError, LogContext] =
    for {
      traceId <- TraceId.randomAlphanumeric[WithError](prefix, 6)
    } yield LogContext(traceId, Map.empty)

}
