package $organization$.util.logging

import cats.effect.{Concurrent, ContextShift, Resource, Sync, Timer}
import io.odin.formatter.Formatter
import io.odin.formatter.options.ThrowableFormat
import io.odin.syntax._
import io.odin.{Level, Logger}

object Loggers {

  def consoleContextLogger[F[_]: Sync: Timer: TraceProvider](minLevel: Level): Logger[F] =
    io.odin.consoleLogger[F](formatter, minLevel).withContext

  def fileContextLogger[F[_]: Concurrent: ContextShift: Timer: TraceProvider](
      fileName: String,
      minLevel: Level
  ): Resource[F, Logger[F]] =
    io.odin.asyncFileLogger[F](fileName, formatter, minLevel = minLevel).withContext

  def envLogLevel(variable: String): Option[Level] =
    sys.env.get(variable).map(_.toUpperCase).flatMap(stringToLevel.lift)

  private val formatter: Formatter = Formatter.create(
    ThrowableFormat(ThrowableFormat.Depth.Full, ThrowableFormat.Indent.Fixed(4)),
    colorful = false
  )

  private val stringToLevel: PartialFunction[String, Level] = {
    case "TRACE" => Level.Trace
    case "DEBUG" => Level.Debug
    case "INFO"  => Level.Info
    case "WARN"  => Level.Warn
    case "ERROR" => Level.Error
  }

}
