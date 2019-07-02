package $organization$.persistence

import cats.effect._
import com.typesafe.config.Config
import doobie.hikari.HikariTransactor
import $organization$.persistence.mongo.{MongoConfig, MongoLoader}
import $organization$.persistence.postgres.{PostgresConfig, TransactorLoader}
import $organization$.util.error.ErrorHandle
import $organization$.util.syntax.config._
import $organization$.util.logging.TraceProvider
import org.mongodb.scala.MongoDatabase

class PersistenceModuleLoader[F[_]: Sync: ErrorHandle: TraceProvider](
    mongoLoader: MongoLoader[F],
    transactorLoader: TransactorLoader[F]
) {

  def load(rootConfig: Config): Resource[F, PersistenceModule[F]] =
    for {
      mongoDatabase <- loadMongoDatabase(rootConfig)
      transactor    <- loadTransactor(rootConfig)
    } yield PersistenceModule(mongoDatabase, transactor)

  private[persistence] def loadMongoDatabase(rootConfig: Config): Resource[F, MongoDatabase] =
    for {
      mongoConfig <- Resource.liftF(rootConfig.loadF[F, MongoConfig]("application.persistence.mongodb"))
      db          <- mongoLoader.createAndVerify(mongoConfig)
    } yield db

  private[persistence] def loadTransactor(rootConfig: Config): Resource[F, HikariTransactor[F]] =
    for {
      config <- Resource.liftF(rootConfig.loadF[F, PostgresConfig]("application.persistence.postgres"))
      db     <- transactorLoader.createAndVerify(config)
    } yield db

}

object PersistenceModuleLoader {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider] =
    new PersistenceModuleLoader[F](MongoLoader.default, TransactorLoader.default)

}
