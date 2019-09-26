package $organization$.util.logging

import java.util.UUID

import cats.effect.Sync
import com.typesafe.scalalogging.CanLog

import scala.util.Random

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
final case class TraceId(value: String) {

  def subId(id: String): TraceId = copy(value + "#" + id)

  def subId(id: Int): TraceId = subId(id.toString)

}

object TraceId {

  def randomUuid[F[_]: Sync]: F[TraceId] =
    Sync[F].delay(TraceId(UUID.randomUUID().toString))

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def randomAlphanumeric[F[_]: Sync](prefix: String, length: Int = 6): F[TraceId] =
    Sync[F].delay(TraceId(prefix + "-" + Random.alphanumeric.take(length).map(_.toLower).mkString))

  implicit object CanLogTraceId extends CanLog[TraceId] {
    override def logMessage(originalMsg: String, a: TraceId): String = s"[\${a.value}] \$originalMsg"
  }

}
