package $organization$.util
package logging

import cats.effect.Sync
import cats.syntax.flatMap._
import $organization$.util.error._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
class TracedLogger[F[_]](logger: LoggerTakingImplicit[TraceId])(implicit F: Sync[F], traceProvider: TraceProvider[F]) {

  def info(value: String): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(logger.info(value))
  }

  def error(value: String): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(logger.error(value))
  }

  def error(value: String, cause: Throwable): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(logger.error(value, cause))
  }

  def error(value: String, error: RaisedError): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(withError(value, error))
  }

  def warn(value: String): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(logger.warn(value))
  }

  private def withError(message: String, error: RaisedError)(implicit traceId: TraceId): Unit =
    error.error.fold(AppError.getException) match {
      case Some(cause) =>
        logger.error(message, cause)

      case None =>
        logger.error(message)
    }

}

object TracedLogger {

  def apply[F[_]](implicit instance: TracedLogger[F]): TracedLogger[F] = instance

  def create[F[_]: Sync: TraceProvider](clazz: Class[_]): TracedLogger[F] =
    new TracedLogger[F](Logger.takingImplicit[TraceId](clazz))

}
