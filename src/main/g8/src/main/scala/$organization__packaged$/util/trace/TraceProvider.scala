package $organization$.util.trace

import cats.mtl.Local

type TraceProvider[F[_]] = Local[F, LogContext]

object TraceProvider {
  def apply[F[_]](implicit instance: TraceProvider[F]): TraceProvider[F] = instance
}
