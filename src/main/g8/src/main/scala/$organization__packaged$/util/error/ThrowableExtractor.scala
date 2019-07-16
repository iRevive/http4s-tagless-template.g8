package $organization$.util.error

import shapeless.{:+:, CNil, Coproduct, Lazy}

trait ThrowableExtractor[E] {
  def select(e: E): Option[Throwable]
}

object ThrowableExtractor {
  def apply[E](implicit ev: ThrowableExtractor[E]): ThrowableExtractor[E] = ev

  // \$COVERAGE-OFF\$
  implicit val cnilThrowableExtractor: ThrowableExtractor[CNil] =
    cnil => sys.error(s"Unreachable \$cnil")
  // \$COVERAGE-ON\$

  implicit def coproductThrowableExtractor[H, T <: Coproduct](
      implicit h: Lazy[ThrowableExtractor[H]],
      t: ThrowableExtractor[T]
  ): ThrowableExtractor[H :+: T] =
    value => value.eliminate(h.value.select, t.select)
}

trait EmptyThrowableExtractor[E] extends ThrowableExtractor[E] {
  override def select(e: E): Option[Throwable] = None
}

object EmptyThrowableExtractor {
  def create[E]: EmptyThrowableExtractor[E] = new EmptyThrowableExtractor[E] {}
}
