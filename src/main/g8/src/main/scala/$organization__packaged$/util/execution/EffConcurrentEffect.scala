package $organization$.util
package execution

import cats.data.{EitherT, Kleisli}
import cats.effect._
import cats.syntax.either._
import $organization$.util.error.RaisedError
import $organization$.util.logging.TraceId
import monix.eval.Task

// \$COVERAGE-OFF\$
final class EffConcurrentEffect(implicit F: ConcurrentEffect[Task]) extends ConcurrentEffect[Eff] {

  private val CE: Concurrent[Eff] = Concurrent.catsKleisliConcurrent

  private def asCallback[A](e: Either[Throwable, Either[RaisedError, A]]): Either[Throwable, A] =
    e.flatMap(x => x.leftMap(_.toException))

  override def runCancelable[A](fa: Eff[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[Eff]] = {
    val f = for {
      traceId <- TraceId.randomAlphanumeric[Task]("concurrent-effect-runCancelable")
      result       <- fa.run(traceId).value
    } yield result

    F.runCancelable(f)(cb.compose(asCallback)).map(v => Kleisli.liftF(EitherT.liftF(v)(F)))
  }

  override def runAsync[A](fa: Eff[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = {
    val f = for {
      traceId <- TraceId.randomAlphanumeric[Task]("concurrent-effect-runAsync")
      result  <- fa.run(traceId).value
    } yield result

    F.runAsync(f)(cb.compose(asCallback))
  }

  override def start[A](fa: Eff[A]): Eff[Fiber[Eff, A]] =
    CE.start(fa)

  override def racePair[A, B](fa: Eff[A], fb: Eff[B]): Eff[Either[(A, Fiber[Eff, B]), (Fiber[Eff, A], B)]] =
    CE.racePair(fa, fb)

  override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Eff[A] =
    CE.async(k)

  override def asyncF[A](k: (Either[Throwable, A] => Unit) => Eff[Unit]): Eff[A] =
    CE.asyncF(k)

  override def suspend[A](thunk: => Eff[A]): Eff[A] =
    CE.suspend(thunk)

  override def bracketCase[A, B](acquire: Eff[A])(use: A => Eff[B])(release: (A, ExitCase[Throwable]) => Eff[Unit]): Eff[B] =
    CE.bracketCase(acquire)(use)(release)

  override def raiseError[A](e: Throwable): Eff[A] =
    CE.raiseError(e)

  override def handleErrorWith[A](fa: Eff[A])(f: Throwable => Eff[A]): Eff[A] =
    CE.handleErrorWith(fa)(f)

  override def pure[A](x: A): Eff[A] =
    CE.pure(x)

  override def flatMap[A, B](fa: Eff[A])(f: A => Eff[B]): Eff[B] =
    CE.flatMap(fa)(f)

  override def tailRecM[A, B](a: A)(f: A => Eff[Either[A, B]]): Eff[B] =
    CE.tailRecM(a)(f)

}
// \$COVERAGE-ON\$
