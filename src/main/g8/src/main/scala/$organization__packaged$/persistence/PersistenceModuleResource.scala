package $organization$.persistence

import cats.effect._
import cats.syntax.functor._
import com.typesafe.config.Config
import doobie.hikari.HikariTransactor
import $organization$.persistence.postgres.{PostgresConfig, TransactorResource}
import $organization$.util.error.{ErrorHandle, ErrorIdGen}
import $organization$.util.syntax.config._
import $organization$.util.syntax.logging._
import $organization$.util.logging.{TraceProvider, TracedLogger}
import org.flywaydb.core.Flyway

class PersistenceModuleResource[F[_]: Sync: ErrorHandle: TraceProvider: ErrorIdGen](
    transactorResource: TransactorResource[F]
) {

  def create(rootConfig: Config, blocker: Blocker): Resource[F, PersistenceModule[F]] =
    for {
      transactor <- createTransactor(rootConfig, blocker)
    } yield PersistenceModule(transactor)

  private[persistence] def createTransactor(rootConfig: Config, blocker: Blocker): Resource[F, HikariTransactor[F]] =
    for {
      config <- Resource.liftF(rootConfig.loadF[F, PostgresConfig]("application.persistence.postgres"))
      db     <- transactorResource.createAndVerify(config, blocker)
      _      <- Resource.liftF(logger.info(log"Run migration [\${config.runMigration}]"))
      _      <- Resource.liftF(Sync[F].whenA(config.runMigration)(runFlywayMigration(db)))
    } yield db

  private def runFlywayMigration(transactor: HikariTransactor[F]): F[Unit] =
    transactor.configure { dataSource =>
      Sync[F].delay(Flyway.configure().dataSource(dataSource).load().migrate()).void
    }

  private val logger: TracedLogger[F] = TracedLogger.create[F](getClass)

}

object PersistenceModuleResource {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider: ErrorIdGen] =
    new PersistenceModuleResource[F](TransactorResource.default)

}
