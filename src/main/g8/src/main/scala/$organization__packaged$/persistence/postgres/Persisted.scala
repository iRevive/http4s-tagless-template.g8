package $organization$.persistence.postgres

import java.time.Instant

import cats.Eq
import $organization$.util.logging.Loggable

@scalaz.deriving(Eq, Loggable)
final case class Persisted[PK, A](
    id: PK,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Option[Instant],
    entity: A
)
