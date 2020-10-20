package $organization$.util

import cats.mtl.Local

package object logging {

  type TraceProvider[F[_]] = Local[F, TraceId]

  object TraceProvider {
    def apply[F[_]](implicit instance: TraceProvider[F]): TraceProvider[F] = instance
  }

}
