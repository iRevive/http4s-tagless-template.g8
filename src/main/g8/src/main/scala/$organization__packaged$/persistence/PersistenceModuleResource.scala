package $organization$.persistence

import cats.effect._
import cats.syntax.applicative._
import cats.syntax.functor._
import com.typesafe.config.Config
import doobie.hikari.HikariTransactor
import $organization$.persistence.postgres.{PostgresConfig, TransactorResource}
import $organization$.util.error.{ErrorHandle, ErrorIdGen}
import $organization$.util.syntax.config._
import $organization$.util.logging.TraceProvider
import io.odin.Logger
import io.odin.syntax._
import org.flywaydb.core.Flyway

class PersistenceModuleResource[F[_]: Sync: ErrorHandle: TraceProvider: ErrorIdGen: Logger](
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
      _      <- Resource.liftF(logger.info(render"Run migration [\${config.runMigration}]"))
      _      <- Resource.liftF(runFlywayMigration(db).whenA(config.runMigration))
    } yield db

  private def runFlywayMigration(transactor: HikariTransactor[F]): F[Unit] =
    transactor.configure(dataSource => Sync[F].delay(Flyway.configure().envVars().dataSource(dataSource).load().migrate()).void)

  private val logger: Logger[F] = Logger[F]

}

object PersistenceModuleResource {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider: ErrorIdGen: Logger] =
    new PersistenceModuleResource[F](TransactorResource.default)

}
