package $organization$.util.logging

import java.util.UUID

import cats.Functor
import cats.effect.Sync
import cats.syntax.functor._
import $organization$.util.logging.TraceId._
import com.typesafe.scalalogging.CanLog

import scala.util.Random

sealed trait TraceId {

  def child(traceId: TraceId): TraceId  = TraceIdPath(this, traceId)
  def childText(value: String): TraceId = TraceIdPath(this, Text(value))

  def align: List[TraceId] = {
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def loop(elem: TraceId, out: List[TraceId]): List[TraceId] =
      elem match {
        case TraceIdPath(h, t) => loop(t, loop(h, out))
        case v: ApiRoute       => out :+ v
        case v: Text           => out :+ v
        case v: Uuid           => out :+ v
      }

    loop(this, Nil)
  }

  def render(separator: String): String = {
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def loop(elem: TraceId, out: List[String]): List[String] =
      elem match {
        case TraceIdPath(h, t) => loop(t, loop(h, out))
        case ApiRoute(value)   => out :+ value
        case Text(value)       => out :+ value
        case Uuid(value)       => out :+ value.toString
      }

    loop(this, Nil).mkString(separator)
  }

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

  final case class ApiRoute(value: String) extends TraceId
  final case class Text(value: String)     extends TraceId
  final case class Uuid(value: UUID)       extends TraceId

  def randomUuid[F[_]: Sync]: F[Uuid] =
    Sync[F].delay(Uuid(UUID.randomUUID()))

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments", "org.wartremover.warts.Overloading"))
  def randomAlphanumeric[F[_]: Sync](prefix: String, length: Int = 6): F[TraceId] =
    childF(TraceId.Text(prefix), randomAlphanumeric(length))

  def randomAlphanumeric[F[_]: Sync](length: Int): F[Text] =
    Sync[F].delay(Text(Random.alphanumeric.take(length).map(_.toLower).mkString))

  def childF[F[_]: Functor, A <: TraceId](parent: TraceId, traceId: F[A]): F[TraceId] =
    traceId.map(parent.child)

  implicit object CanLogTraceId extends CanLog[TraceId] {
    override def logMessage(originalMsg: String, a: TraceId): String = s"[\${a.render("-")}] \$originalMsg"
  }

}
