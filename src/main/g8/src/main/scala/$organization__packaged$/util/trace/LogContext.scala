package $organization$.util.trace

import io.odin.loggers.HasContext

final case class LogContext(traceId: TraceId, extra: Map[String, String])

object LogContext {
  implicit val hasContext: HasContext[LogContext] = { case LogContext(traceId, extra) =>
    extra ++ Map("traceId" -> TraceId.render(traceId, "#"))
  }
}
