package $organization$.test

import cats.Functor
import cats.syntax.functor._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.Coercible

import scala.util.Random

trait GenRandom[A] {
  def gen: A
}

object GenRandom {

  def apply[A](implicit ev: GenRandom[A]): GenRandom[A] = ev

  def instance[A](f: () => A): GenRandom[A] = new GenRandom[A] {
    override def gen: A = f()
  }

  def list[A: GenRandom](length: Int): GenRandom[List[A]] =
    instance(() => List.fill(length)(GenRandom[A].gen))

  implicit val genRandomFunctor: Functor[GenRandom] = new Functor[GenRandom] {
    override def map[A, B](fa: GenRandom[A])(f: A => B): GenRandom[B] =
      instance(() => f(fa.gen))
  }

  implicit val stringRandom: GenRandom[String] =
    instance(() => Random.alphanumeric.take(10).map(_.toLower).mkString)

  implicit val nonEmptyStringRandom: GenRandom[NonEmptyString] = GenRandom[String].map(NonEmptyString.unsafeFrom)

  implicit def coercibleRandom[R, N](implicit ev: Coercible[GenRandom[R], GenRandom[N]], R: GenRandom[R]): GenRandom[N] =
    ev(R)

}

object GenRandomDerivation {

  import magnolia._

  type Typeclass[T] = GenRandom[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): GenRandom[T] = GenRandom.instance { () =>
    ctx.construct(param => param.typeclass.gen)
  }

  def dispatch[T](ctx: SealedTrait[Typeclass, T]): Typeclass[T] = GenRandom.instance { () =>
    ctx.subtypes.toList match {
      case Nil    => sys.error(s"No subtypes for \$ctx")
      case values => values(Random.nextInt(values.length)).typeclass.gen
    }
  }

  implicit def derive[T]: Typeclass[T] = macro Magnolia.gen[T]

}
