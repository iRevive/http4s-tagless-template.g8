package $organization$.util.logging

import cats.effect.{Concurrent, ContextShift, Timer}
import io.odin.formatter.Formatter
import io.odin.formatter.options.ThrowableFormat
import io.odin.syntax._
import io.odin.{Level, Logger}

object Loggers {

  def createContextLogger[F[_]: Concurrent: ContextShift: Timer: TraceProvider](minLevel: Level): Logger[F] =
    io.odin.consoleLogger[F](formatter, minLevel).withContext

  def envLogLevel: Option[Level] =
    sys.env.get("LOG_LEVEL").map(_.toUpperCase).map {
      case "TRACE" => Level.Trace
      case "DEBUG" => Level.Debug
      case "INFO"  => Level.Info
      case "WARN"  => Level.Warn
      case "ERROR" => Level.Error
    }

  private val formatter: Formatter = Formatter.create(
    ThrowableFormat(ThrowableFormat.Depth.Full, ThrowableFormat.Indent.Fixed(4)),
    colorful = false
  )

}
