package $organization$

import cats.data.{EitherT, Kleisli}
import cats.effect._
import cats.syntax.either._
import $organization$.util.error.BaseError
import $organization$.util.logging.TraceId
import monix.eval.Task

package object util {

  type Traced[F[_], A] = Kleisli[F, TraceId, A]

  type TracedResultT[A] = Traced[EitherT[Task, BaseError, ?], A]

  def concurrentEffect(implicit F: ConcurrentEffect[Task]): ConcurrentEffect[TracedResultT] = {
    new ConcurrentEffect[TracedResultT] {

      private implicit val CE: Concurrent[TracedResultT] = Concurrent.catsKleisliConcurrent

      override def runCancelable[A](fa: TracedResultT[A])(
        cb: Either[Throwable, A] => IO[Unit]
      ): SyncIO[CancelToken[TracedResultT]] =
        F.runCancelable(fa.run(TraceId.randomAlphanumeric("concurrent-effect")).value)(
            cb.compose(_.right.flatMap(x => x.leftMap(_.toRuntimeException)))
          )
          .map(v => Kleisli.liftF(EitherT.liftF(v)(F)))

      override def runAsync[A](fa: TracedResultT[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] =
        F.runAsync(fa.run(TraceId.randomAlphanumeric("concurrent-effect")).value)(
          cb.compose(_.right.flatMap(x => x.leftMap(_.toRuntimeException)))
        )

      override def start[A](fa: TracedResultT[A]): TracedResultT[Fiber[TracedResultT, A]] =
        CE.start(fa)

      override def racePair[A, B](
          fa: TracedResultT[A],
          fb: TracedResultT[B]
      ): TracedResultT[Either[(A, Fiber[TracedResultT, B]), (Fiber[TracedResultT, A], B)]] =
        CE.racePair(fa, fb)

      override def async[A](k: (Either[Throwable, A] => Unit) => Unit): TracedResultT[A] =
        CE.async(k)

      override def asyncF[A](k: (Either[Throwable, A] => Unit) => TracedResultT[Unit]): TracedResultT[A] =
        CE.asyncF(k)

      override def suspend[A](thunk: => TracedResultT[A]): TracedResultT[A] =
        CE.suspend(thunk)

      override def bracketCase[A, B](acquire: TracedResultT[A])(use: A => TracedResultT[B])(
          release: (A, ExitCase[Throwable]) => TracedResultT[Unit]): TracedResultT[B] =
        CE.bracketCase(acquire)(use)(release)

      override def raiseError[A](e: Throwable): TracedResultT[A] =
        CE.raiseError(e)

      override def handleErrorWith[A](fa: TracedResultT[A])(f: Throwable => TracedResultT[A]): TracedResultT[A] =
        CE.handleErrorWith(fa)(f)

      override def pure[A](x: A): TracedResultT[A] =
        CE.pure(x)

      override def flatMap[A, B](fa: TracedResultT[A])(f: A => TracedResultT[B]): TracedResultT[B] =
        CE.flatMap(fa)(f)

      override def tailRecM[A, B](a: A)(f: A => TracedResultT[Either[A, B]]): TracedResultT[B] =
        CE.tailRecM(a)(f)
    }
  }

}
