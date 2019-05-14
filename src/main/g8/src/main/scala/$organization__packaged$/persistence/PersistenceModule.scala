package $organization$.persistence

import org.mongodb.scala.MongoDatabase
import doobie.hikari.HikariTransactor

final case class PersistenceModule[F[_]](mongoDatabase: MongoDatabase, transactor: HikariTransactor[F])
