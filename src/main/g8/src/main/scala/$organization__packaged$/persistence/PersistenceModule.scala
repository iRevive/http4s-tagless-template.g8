package $organization$.persistence

import doobie.hikari.HikariTransactor

final case class PersistenceModule[F[_]](transactor: HikariTransactor[F])
