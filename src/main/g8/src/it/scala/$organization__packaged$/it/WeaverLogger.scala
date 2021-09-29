package $organization$.it

import cats.data.OptionT
import cats.effect.{Ref, Sync}
import cats.syntax.applicative.*
import cats.syntax.functor.*
import cats.syntax.order.*
import io.odin.formatter.Formatter
import io.odin.loggers.DefaultLogger
import io.odin.{Level, Logger, LoggerMessage}

final case class WeaverLogger[F[_]: Sync](
    formatter: Formatter,
    backend: Ref[F, Map[WeaverLogger.Key, weaver.Log[F]]],
    fallback: Logger[F],
    override val minLevel: Level
) extends DefaultLogger[F](minLevel) {

  def setWeaverLog(key: WeaverLogger.Key, log: weaver.Log[F]): F[Unit] =
    backend.modify(v => (v.updated(key, log), ()))

  def removeWeaverLog(key: WeaverLogger.Key): F[Unit] =
    backend.modify(v => (v.removed(key), ()))

  def submit(msg: LoggerMessage): F[Unit] = {
    val cleanMessage = msg.copy(context = Map.empty) // todo remove only WeaverLogger.ContextKey ?

    def fallbackLog = fallback.log(cleanMessage).whenA(msg.level >= fallback.minLevel)

    OptionT
      .fromOption[F](msg.context.get(WeaverLogger.ContextKey))
      .flatMapF(id => backend.get.map(_.get(WeaverLogger.Key(id))))
      .foldF(fallbackLog)(weaverLogger => weaverLogger.log(toEntry(cleanMessage)))
  }

  def withMinimalLevel(level: Level): Logger[F] =
    copy(minLevel = level)

  private def toEntry(msg: LoggerMessage): weaver.Log.Entry =
    weaver.Log.Entry(
      msg.timestamp,
      msg.message.value,
      msg.context,
      transformLogLevel(msg.level),
      msg.exception,
      weaver.SourceLocation(msg.position.enclosureName, msg.position.fileName, msg.position.line)
    )

  private def transformLogLevel: Level => weaver.Log.Level = {
    case Level.Trace => weaver.Log.debug
    case Level.Debug => weaver.Log.debug
    case Level.Info  => weaver.Log.info
    case Level.Warn  => weaver.Log.warn
    case Level.Error => weaver.Log.error
  }

}

object WeaverLogger {

  val ContextKey = "weaver-logger"

  final case class Key(value: String)

  def create[F[_]: Sync](formatter: Formatter, fallback: Logger[F], minLevel: Level): F[WeaverLogger[F]] =
    for {
      ref <- Ref.of(Map.empty[Key, weaver.Log[F]])(Ref.Make.syncInstance)
    } yield WeaverLogger(formatter, ref, fallback, minLevel)

}
