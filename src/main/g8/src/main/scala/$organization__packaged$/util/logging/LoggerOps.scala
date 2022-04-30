package $organization$.util.logging

import $organization$.util.error.ThrowableSelect
import io.odin.Logger

trait ToLoggerOps {
  final implicit def toLoggerOps[F[_]](logger: Logger[F]): LoggerOps[F] = new LoggerOps[F](logger)
}

final class LoggerOps[F[_]](private val logger: Logger[F]) extends AnyVal {

  def error[E: ThrowableSelect](message: => String, error: E): F[Unit] =
    ThrowableSelect[E].select(error) match {
      case Some(cause) => logger.error(message, cause)
      case None        => logger.error(message)
    }

}
