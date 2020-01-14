package $organization$.util.logging

import cats.effect.{Concurrent, ContextShift, Resource, Timer}
import io.odin.syntax._
import io.odin.{Level, Logger}

object Loggers {

  def createContextLogger[F[_]: Concurrent: ContextShift: Timer: TraceProvider](minLevel: Level): Resource[F, Logger[F]] =
    io.odin.consoleLogger[F](minLevel = minLevel).withContext.withAsync()

}
