package $organization$.util

import cats.~>
import $organization$.util.logging.TraceId
import monix.eval.Task

trait TracedLike[F[_], G[_]] {

  def arrow(traceId: TraceId): F ~> G

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
