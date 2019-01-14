package $organization$.util

import cats.~>

final class MapKOps[F[_], A](value: F[A]) {
  def mapK[G[_]](f: F ~> G): G[A] = f(value)
}

trait ToMapKOps {
  final implicit def toMapKOps[F[_], A](value: F[A]): MapKOps[F, A] = new MapKOps(value)
}
