package $organization$.util
package logging

import cats.effect.Sync
import cats.syntax.flatMap._
import $organization$.util.error._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}

import scala.util.control.NonFatal

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

  def error(value: String, error: BaseError): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(withError(value, error))
  }

  def warn(value: String): F[Unit] = traceProvider.ask.flatMap { implicit traceId =>
    F.delay(logger.warn(value))
  }

  private def withError(message: String, error: BaseError)(implicit traceId: TraceId): Unit = {
    error match {
      case ThrowableError(cause) =>
        logger.error(message, cause)

      case NonFatal(cause) =>
        logger.error(message, cause)

      case _ =>
        logger.error(message)
    }
  }

}

object TracedLogger {

  def create[F[_]: Sync: TraceProvider](clazz: Class[_]): TracedLogger[F] =
    new TracedLogger[F](Logger.takingImplicit[TraceId](clazz))

}
