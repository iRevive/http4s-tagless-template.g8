package $organization$.util

import cats.effect.Resource
import cats.effect.Resource.{Allocate, Bind, Suspend}
import cats.implicits._
import cats.{~>, Functor}

/**
  * Taken from https://github.com/tpolecat/skunk/blob/master/modules/core/src/main/scala/syntax/ResourceOps.scala
  */
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

  def arrow[F[_]: Functor, G[_]](fk: F ~> G): Resource[F, ?] ~> Resource[G, ?] =
    new (Resource[F, ?] ~> Resource[G, ?]) {
      override def apply[A](fa: Resource[F, A]): Resource[G, A] = {
        mapK(fa)(fk)
      }
    }

}
