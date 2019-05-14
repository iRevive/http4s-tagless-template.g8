package $organization$.persistence.postgres

import $organization$.util.logging.TraceId
import com.typesafe.scalalogging.Logger
import doobie.util.log.{ExecFailure, LogHandler, ProcessingFailure, Success}

object TracedLogHandler {

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def logHandler(implicit traceId: TraceId): LogHandler =
    LogHandler {
      case Success(s, a, e1, e2) =>
        logger.info(
          s"""Successful Statement Execution:
             |
             |  \${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
             |
             | arguments = [\${a.mkString(", ")}]
             |   elapsed = \${e1.toMillis} ms exec + \${e2.toMillis} ms processing (\${(e1 + e2).toMillis} ms total)
          """.stripMargin
        )

      case ProcessingFailure(s, a, e1, e2, t) =>
        logger.error(
          s"""Failed Resultset Processing:
              |
              |  \${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
              |
              | arguments = [\${a.mkString(", ")}]
              |   elapsed = \${e1.toMillis} ms exec + \${e2.toMillis} ms processing (failed) (\${(e1 + e2).toMillis} ms total)
              |   failure = \${t.getMessage}
           """.stripMargin
        )

      case ExecFailure(s, a, e1, t) =>
        logger.error(
          s"""Failed Statement Execution:
              |
              |  \${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
              |
              | arguments = [\${a.mkString(", ")}]
              |   elapsed = \${e1.toMillis} ms exec (failed)
              |   failure = \${t.getMessage}
          """.stripMargin
        )

    }

  private val logger = Logger.takingImplicit[TraceId](getClass)

}