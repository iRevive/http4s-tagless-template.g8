package $organization$.util
package logging

import cats.effect.Sync
import cats.syntax.flatMap._
import $organization$.util.error._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
class TracedLogger[F[_]](logger: LoggerTakingImplicit[TraceId])(implicit F: Sync[F], traceProvider: TraceProvider[F]) {

  def info(message: => String): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(logger.info(message))
  }

  def warn(message: => String): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(logger.warn(message))
  }

  def error(message: => String): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(logger.error(message))
  }

  def error[E: ThrowableSelect](message: => String, error: E): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay {
      ThrowableSelect[E].select(error) match {
        case Some(cause) => logger.error(message, cause)
        case None        => logger.error(message)
      }
    }
  }

}

object TracedLogger {

  def apply[F[_]](implicit instance: TracedLogger[F]): TracedLogger[F] = instance

  def create[F[_]: Sync: TraceProvider](clazz: Class[_]): TracedLogger[F] =
    new TracedLogger[F](Logger.takingImplicit[TraceId](clazz))

}
