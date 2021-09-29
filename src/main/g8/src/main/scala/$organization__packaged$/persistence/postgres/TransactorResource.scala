package $organization$.persistence.postgres

import cats.effect.*
import cats.mtl.syntax.local.*
import cats.syntax.applicative.*
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.functor.*
import $organization$.util.ConfigSource
import $organization$.util.error.{ErrorChannel, RaisedError}
import $organization$.util.execution.Retry
import $organization$.util.instances.render.*
import $organization$.util.trace.{LogContext, TraceId, TraceProvider}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import eu.timepit.refined.auto.*
import io.odin.Logger
import io.odin.syntax.*
import org.flywaydb.core.Flyway
import retry.mtl.syntax.all.*

import scala.concurrent.duration.*

class TransactorResource[F[_]: Async: ErrorChannel: TraceProvider: Logger] {

  def createAndVerify(config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      _  <- Resource.eval(logger.info(render"Creating Postgres module with config \$config"))
      xa <- createFromConfig(config)
      _  <- Resource.eval(logger.info("Verifying Postgres connection"))
      _  <- Resource.eval(verifyConnection(config, xa))
    } yield xa

  private def createFromConfig(config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- HikariTransactor.newHikariTransactor[F](config.driver, config.uri, config.user, config.password, ce)
    } yield xa

  private def verifyConnection(config: PostgresConfig, transactor: HikariTransactor[F]): F[Unit] =
    ErrorChannel[F]
      .retryMtl(
        fa = verifyConnectionOnce(transactor, config.connectionAttemptTimeout),
        policy = Retry.makePolicy(config.retryPolicy),
        onError = Retry.logErrors(logger)
      )
      .local[LogContext](ctx => ctx.copy(traceId = ctx.traceId.child(TraceId.Const("verify-postgres-connection"))))

  private[postgres] def verifyConnectionOnce(transactor: HikariTransactor[F], timeout: FiniteDuration): F[Unit] = {
    import doobie.implicits.*

    val query = sql"""SELECT 1""".query[Int].nel

    val attempt = query
      .transact(transactor)
      .handleErrorWith(e => ErrorChannel[F].raise(PostgresError.UnavailableConnection(e)))
      .void

    val timeoutTo: F[Unit] = ErrorChannel[F].raise(
      PostgresError.ConnectionAttemptTimeout(render"Failed attempt to acquire Postgres connection in [\$timeout]")
    )

    Async[F].timeoutTo(attempt, timeout, timeoutTo)
  }

  private val logger: Logger[F] = Logger[F]

}

object TransactorResource {

  def create[F[_]: Async: ErrorChannel: TraceProvider: Logger](
      config: ConfigSource[F]
  ): Resource[F, HikariTransactor[F]] = {
    def runFlywayMigration(transactor: HikariTransactor[F]): F[Unit] =
      transactor.configure(dataSource => Sync[F].delay(Flyway.configure().envVars().dataSource(dataSource).load().migrate()).void)

    val transactorResource = new TransactorResource[F]

    for {
      cfg <- Resource.eval(config.get[PostgresConfig]("application.persistence.postgres"))
      db  <- transactorResource.createAndVerify(cfg)
      _   <- Resource.eval(Logger[F].info(render"Run migration [\${cfg.runMigration}]"))
      _   <- Resource.eval(runFlywayMigration(db).whenA(cfg.runMigration))
    } yield db
  }

}
