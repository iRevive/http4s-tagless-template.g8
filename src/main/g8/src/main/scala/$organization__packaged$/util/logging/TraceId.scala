package $organization$.util.logging

import java.util.UUID

import cats.Functor
import cats.effect.Sync
import cats.syntax.functor._
import $organization$.util.logging.TraceId._
import com.typesafe.scalalogging.CanLog

import scala.util.Random

sealed trait TraceId {
  def child(traceId: TraceId): TraceId = TraceIdPath(this, traceId)
}

object TraceId {

  object / {
    def unapply(arg: TraceId): Option[(TraceId, TraceId)] =
      arg match {
        case TraceIdPath(p, c) => Some((p, c))
        case _                 => None
      }
  }

  private[TraceId] final case class TraceIdPath(parent: TraceId, child: TraceId) extends TraceId

  final case class ApiRoute(value: String)     extends TraceId
  final case class Const(value: String)        extends TraceId
  final case class Alphanumeric(value: String) extends TraceId
  final case class Uuid(value: UUID)           extends TraceId

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
    def loop(elem: TraceId, out: List[String]): List[String] =
      elem match {
        case TraceIdPath(h, t)   => loop(t, loop(h, out))
        case ApiRoute(value)     => out :+ value
        case Const(value)        => out :+ value
        case Alphanumeric(value) => out :+ value
        case Uuid(value)         => out :+ value.toString
      }

    loop(traceId, Nil).mkString(separator)
  }

  implicit object CanLogTraceId extends CanLog[TraceId] {
    override def logMessage(originalMsg: String, a: TraceId): String = s"[\${TraceId.render(a, "-")}] \$originalMsg"
  }

}
