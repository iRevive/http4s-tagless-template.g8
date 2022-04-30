package $organization$.util.trace

import java.util.UUID

import cats.Functor
import cats.effect.Sync
import cats.syntax.functor.*
import io.odin.loggers.HasContext

import scala.util.Random

enum TraceId {
  case /(parent: TraceId, child: TraceId)
  case ApiRoute(value: String)
  case Const(value: String)
  case Alphanumeric(value: String)
  case Uuid(value: UUID)

  def child(traceId: TraceId): TraceId = /(this, traceId)
}

object TraceId {

  def randomUuid[F[_]: Sync]: F[Uuid] =
    Sync[F].delay(Uuid(UUID.randomUUID()))

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments", "org.wartremover.warts.Overloading"))
  def randomAlphanumeric[F[_]: Sync](prefix: String, length: Int = 6): F[TraceId] =
    childF(Const(prefix), randomAlphanumeric(length))

  def randomAlphanumeric[F[_]: Sync](length: Int): F[Alphanumeric] =
    Sync[F].delay(Alphanumeric(Random.alphanumeric.take(length).map(_.toLower).mkString))

  def childF[F[_]: Functor, A <: TraceId](parent: TraceId, traceId: F[A]): F[TraceId] =
    traceId.map(parent.child)

  def render(traceId: TraceId, separator: String): String = {
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def loop(elem: TraceId, out: Vector[String]): Vector[String] =
      elem match {
        case h / t               => loop(t, loop(h, out))
        case ApiRoute(value)     => out :+ value
        case Const(value)        => out :+ value
        case Alphanumeric(value) => out :+ value
        case Uuid(value)         => out :+ value.toString
      }

    loop(traceId, Vector.empty).mkString(separator)
  }

}
