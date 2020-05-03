package $organization$.util.error

import shapeless.{:+:, CNil, Coproduct, Lazy}

trait ThrowableSelect[E] {
  def select(e: E): Option[Throwable]
}

object ThrowableSelect {

  def apply[E](implicit ev: ThrowableSelect[E]): ThrowableSelect[E] = ev

  sealed abstract class Empty[E] extends ThrowableSelect[E] {
    override def select(e: E): Option[Throwable] = None
  }

  object Empty {
    def create[E]: Empty[E] = new Empty[E] {}
  }

  implicit val raisedErrorThrowableSelect: ThrowableSelect[RaisedError] =
    v => ThrowableSelect[AppError].select(v.error)

  implicit def throwableSelect[E <: Throwable]: ThrowableSelect[E] =
    e => Option(e)

  // \$COVERAGE-OFF\$
  implicit val cnilThrowableSelect: ThrowableSelect[CNil] = _.impossible
  // \$COVERAGE-ON\$

  implicit def coproductThrowableSelect[H, T <: Coproduct](implicit
      h: Lazy[ThrowableSelect[H]],
      t: ThrowableSelect[T]
  ): ThrowableSelect[H :+: T] =
    value => value.eliminate(h.value.select, t.select)
}
