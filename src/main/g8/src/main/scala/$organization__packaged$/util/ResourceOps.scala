package $organization$.util

import cats.effect.Resource
import cats.effect.Resource.{Allocate, Bind, Suspend}
import cats.implicits._
import cats.{~>, Functor}

/**
  * Taken from https://github.com/tpolecat/skunk/blob/master/modules/core/src/main/scala/syntax/ResourceOps.scala
  */
final class ResourceOps[F[_], A](rsrc: Resource[F, A]) {

  def mapK[G[_]](fk: F ~> G)(implicit F: Functor[F]): Resource[G, A] =
    ResourceOps.mapK(rsrc)(fk)

}

object ResourceOps {

  // Really we can implement this if either is a functor, so I flipped a coin.
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def mapK[F[_]: Functor, G[_], A](rsrc: Resource[F, A])(fk: F ~> G): Resource[G, A] =
    rsrc match {
      case Allocate(fa) =>
        Allocate(
          fk(
            fa.map { case (a, f) => (a, f.map(fk(_))) }
          ))
      case Bind(s, fs) => Bind(mapK(s)(fk), (z: Any) => mapK(fs(z))(fk)) // wtf
      case Suspend(fr) => Suspend(fk(fr.map(mapK(_)(fk))))
    }

}

trait ToResourceOps {
  implicit def toResourceOps[F[_], A](rsrc: Resource[F, A]): ResourceOps[F, A] = new ResourceOps(rsrc)
}
