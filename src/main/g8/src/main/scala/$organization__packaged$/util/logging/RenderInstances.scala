package $organization$.util
package logging

import java.time.Instant

import eu.timepit.refined.api.RefType
import io.odin.meta.Render

import scala.concurrent.duration.FiniteDuration

trait RenderInstances {

  implicit val renderInstant: Render[Instant] = Render.fromToString

  implicit val renderFiniteDuration: Render[FiniteDuration] = Render.fromToString

  implicit val renderThrowable: Render[Throwable] = throwable => {
    val className = throwable.getClass.getSimpleName
    val message   = Option(throwable.getMessage).getOrElse("<empty message>")

    s"\$className(\$message)"
  }

  implicit def renderRefined[T, P, F[_, _]](implicit ev: Render[T], refType: RefType[F]): Render[F[T, P]] =
    value => ev.render(refType.unwrap(value))

}

object RenderInstances extends RenderInstances
