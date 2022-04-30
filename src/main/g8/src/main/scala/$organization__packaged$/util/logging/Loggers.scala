package $organization$.util.logging

import java.io.File
import java.util.Locale

import cats.effect.{Async, Resource}
import cats.syntax.applicativeError.*
import $organization$.util.trace.TraceProvider
import io.odin.formatter.Formatter
import io.odin.formatter.options.ThrowableFormat
import io.odin.syntax.*
import io.odin.{Level, Logger}

object Loggers {

  def consoleContextLogger[F[_]: Async: TraceProvider](minLevel: Level): Logger[F] =
    io.odin.consoleLogger[F](formatter, minLevel).withContext

  def consoleContextLoggerAsync[F[_]: Async: TraceProvider](minLevel: Level): Resource[F, Logger[F]] =
    io.odin.consoleLogger[F](formatter, minLevel).withAsync().withContext

  def fileContextLoggerAsync[F[_]: Async: TraceProvider](
      fileName: String,
      minLevel: Level
  ): Resource[F, Logger[F]] =
    for {
      _      <- Resource.eval(Async[F].blocking(new File(fileName).getParentFile.mkdirs()).attempt)
      logger <- io.odin.asyncFileLogger[F](fileName, formatter, minLevel = minLevel).withContext
    } yield logger

  def envLogLevel(variable: String): Option[Level] =
    sys.env.get(variable).map(_.toUpperCase(Locale.ENGLISH)).flatMap(stringToLevel.lift)

  val formatter: Formatter = Formatter.create(
    ThrowableFormat(ThrowableFormat.Depth.Full, ThrowableFormat.Indent.Fixed(4), ThrowableFormat.Filter.NoFilter),
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
