package $organization$.persistence.postgres

import java.time.Instant

import cats.Eq
import $organization$.util.instances.render.*
import io.odin.extras.derivation.render.derived
import io.odin.meta.Render

final case class Persisted[PK, A](
    id: PK,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Option[Instant],
    entity: A
) derives /*Eq, */ Render
