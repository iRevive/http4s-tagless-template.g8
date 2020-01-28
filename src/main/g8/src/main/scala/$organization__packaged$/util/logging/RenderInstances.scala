package $organization$.util.logging

import java.time.Instant

import eu.timepit.refined.api.RefType
import io.estatico.newtype.Coercible
import io.odin.meta.Render
import shapeless.{:+:, CNil, Coproduct, Lazy}

import scala.concurrent.duration.FiniteDuration

trait RenderInstances {

  implicit val renderInstant: Render[Instant] = Render.fromToString

  implicit val renderFiniteDuration: Render[FiniteDuration] = Render.fromToString

  implicit val renderThrowable: Render[Throwable] = throwable => {
    val className = throwable.getClass.getSimpleName
    val message   = Option(throwable.getMessage).getOrElse("<empty message>")

    s"\$className(\$message)"
  }

  implicit def renderCoercible[R, N](implicit ev: Coercible[Render[R], Render[N]], r: Render[R]): Render[N] =
    ev(r)

  implicit def renderRefined[T, P, F[_, _]](implicit ev: Render[T], refType: RefType[F]): Render[F[T, P]] =
    value => ev.render(refType.unwrap(value))

  // \$COVERAGE-OFF\$
  implicit val renderCNil: Render[CNil] = _.impossible
  // \$COVERAGE-ON\$

  implicit def renderCoproduct[H, T <: Coproduct](implicit h: Lazy[Render[H]], t: Render[T]): Render[H :+: T] =
    value => value.eliminate(h.value.render, t.render)

}

object RenderInstances extends RenderInstances