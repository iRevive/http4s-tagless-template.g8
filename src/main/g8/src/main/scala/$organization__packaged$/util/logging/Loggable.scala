package $organization$.util
package logging

import java.time.Instant
import java.util.UUID

import cats.Show
import cats.data.NonEmptyList
import eu.timepit.refined.api.RefType
import io.estatico.newtype.Coercible
import magnolia._
import org.http4s.{Status, Uri}
import shapeless._

import scala.annotation.implicitNotFound
import scala.concurrent.duration.FiniteDuration

@implicitNotFound(
  """
 No Loggable found for type \${A}. Try to implement an implicit Loggable[\${A}].
 You can implement it in \${A} companion class.
    """
)
trait Loggable[A] {
  def show(value: A): String
}

object Loggable extends LoggableInstances {

  def apply[A](implicit instance: Loggable[A]): Loggable[A] = instance

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def fromToString[A]: Loggable[A] = _.toString

  def fromShow[A: Show]: Loggable[A] = Show[A].show

  final case class Shown(override val toString: String) extends AnyVal

  object Shown {
    implicit def mat[A](x: A)(implicit z: Loggable[A]): Shown = Shown(z.show(x))
  }

  trait InterpolatorOps {
    @inline
    implicit def toLoggableInterpolator(sc: StringContext): LoggableInterpolator = new LoggableInterpolator(sc)
  }

  object InterpolatorOps extends InterpolatorOps

  final class LoggableInterpolator(private val sc: StringContext) extends AnyVal {
    def log(args: Loggable.Shown*): String = sc.s(args: _*)
  }

}

trait LoggableInstances {

  implicit val stringLoggable: Loggable[String]                          = Loggable.fromToString
  implicit val intLoggable: Loggable[Int]                                = Loggable.fromToString
  implicit val shortLoggable: Loggable[Short]                            = Loggable.fromToString
  implicit val longLoggable: Loggable[Long]                              = Loggable.fromToString
  implicit val doubleLoggable: Loggable[Double]                          = Loggable.fromToString
  implicit val floatLoggable: Loggable[Float]                            = Loggable.fromToString
  implicit val booleanLoggable: Loggable[Boolean]                        = Loggable.fromToString
  implicit val uuidLoggable: Loggable[UUID]                              = Loggable.fromToString
  implicit val finiteDurationLoggable: Loggable[FiniteDuration]          = Loggable.fromToString
  implicit val instantLoggable: Loggable[Instant]                        = Loggable.fromToString
  implicit val circeJsonLoggable: Loggable[io.circe.Json]                = v => v.noSpaces
  implicit val circeErrorLoggable: Loggable[io.circe.Error]              = Loggable.fromShow
  implicit val circeParsingLoggable: Loggable[io.circe.ParsingFailure]   = Loggable.fromShow
  implicit val circeDecodingLoggable: Loggable[io.circe.DecodingFailure] = Loggable.fromShow
  implicit val statusLoggable: Loggable[Status]                          = Loggable.fromToString
  implicit val uriLoggable: Loggable[Uri]                                = Loggable.fromToString

  implicit val throwableLoggable: Loggable[Throwable] = throwable => {
    val className = throwable.getClass.getSimpleName
    val message   = Option(throwable.getMessage).getOrElse("<empty message>")

    s"\$className(\$message)"
  }

  implicit def listLoggable[A: Loggable]: Loggable[List[A]] = traversableLoggable

  implicit def nelLoggable[A: Loggable]: Loggable[NonEmptyList[A]] =
    value => traversableLoggable[A, List].show(value.toList)

  implicit def optionLoggable[A: Loggable]: Loggable[Option[A]] =
    value => value.fold("None")(Loggable[A].show)

  implicit def refinedLoggable[T, P, F[_, _]](implicit underlying: Loggable[T], refType: RefType[F]): Loggable[F[T, P]] =
    value => underlying.show(refType.unwrap(value))

  def traversableLoggable[A, M[X] <: IterableOnce[X]](implicit ev: Loggable[A]): Loggable[M[A]] =
    value => value.iterator.map(ev.show).mkString("[", ", ", "]")

  implicit def coercibleLoggable[R, N](implicit ev: Coercible[Loggable[R], Loggable[N]], R: Loggable[R]): Loggable[N] =
    ev(R)

  // \$COVERAGE-OFF\$
  implicit val cnilLoggable: Loggable[CNil] = _.impossible
  // \$COVERAGE-ON\$

  implicit def coproductLoggable[H, T <: Coproduct](implicit h: Lazy[Loggable[H]], t: Loggable[T]): Loggable[H :+: T] =
    value => value.eliminate(h.value.show, t.show)

}

object LoggableDerivation {

  type Typeclass[T] = Loggable[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Loggable[T] = value => {
    if (ctx.isValueClass) {
      ctx.parameters.headOption.fold("")(param => param.typeclass.show(param.dereference(value)))
    } else {
      val paramStrings = ctx.parameters.flatMap { param =>
        param.dereference(value) match {
          case v: Option[_] if v.isEmpty =>
            Nil

          case v: Seq[_] if v.isEmpty =>
            Nil

          case other =>
            List(s"\${param.label} = \${param.typeclass.show(other)}")
        }
      }

      s"\${ctx.typeName.short}(\${paramStrings.mkString(", ")})"
    }
  }

  def dispatch[T](ctx: SealedTrait[Typeclass, T]): Typeclass[T] = value => {
    ctx.dispatch(value)(sub => sub.typeclass.show(sub.cast(value)))
  }

  implicit def derive[T]: Typeclass[T] = macro Magnolia.gen[T]

}
