package $organization$.util

import cats.~>
import $organization$.util.logging.TraceId
import monix.eval.Task

trait TracedLike[F[_], G[_]] {

  def transformer(traceId: TraceId): F ~> G

}

object TracedLike {

  def apply[F[_], G[_]](implicit instance: TracedLike[F, G]): TracedLike[F, G] = instance

  implicit val tracedResultToTask: TracedLike[TracedResultT[Task, ?], Task] = (traceId: TraceId) => {
    new (TracedResultT[Task, ?] ~> Task) {
      override def apply[A](fa: TracedResultT[Task, A]): Task[A] =
        fa.run(traceId).leftSemiflatMap(e => Task.raiseError(e.toRuntimeException)).merge
    }
  }

}

final class TracedLikeOps[F[_], A](private val underlying: F[A]) extends AnyVal {

  def to[G[_]](implicit traceId: TraceId, t: TracedLike[F, G]): G[A] = t.transformer(traceId).apply(underlying)

  def to_[G[_]](traceId: TraceId)(implicit t: TracedLike[F, G]): G[A] = t.transformer(traceId).apply(underlying)

}

trait ToTracedLikeOps {

  implicit def toTracedLikeOps[F[_], A](underlying: F[A]): TracedLikeOps[F, A] =
    new TracedLikeOps[F, A](underlying)

}
