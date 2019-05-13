package $organization$.util

import cats.mtl.ApplicativeLocal

package object logging {

  type TraceProvider[F[_]] = ApplicativeLocal[F, TraceId]

  object TraceProvider {
    def apply[F[_]](implicit instance: TraceProvider[F]): TraceProvider[F] = instance
  }

}
