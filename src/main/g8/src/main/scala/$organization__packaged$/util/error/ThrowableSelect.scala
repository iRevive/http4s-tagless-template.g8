package $organization$.util
package error

import cats.Contravariant

trait ThrowableSelect[E] {
  def select(e: E): Option[Throwable]
}

object ThrowableSelect {

  def apply[E](using ev: ThrowableSelect[E]): ThrowableSelect[E] = ev

  sealed abstract class Empty[E] extends ThrowableSelect[E] {
    override def select(e: E): Option[Throwable] = None
  }

  object Empty {
    def create[E]: Empty[E] = new Empty[E] {}

    given derived[A]: ThrowableSelect.Empty[A] = create[A]
  }

  given Contravariant[ThrowableSelect] with {
    def contramap[A, B](fa: ThrowableSelect[A])(f: B => A): ThrowableSelect[B] = error => fa.select(f(error))
  }

}
