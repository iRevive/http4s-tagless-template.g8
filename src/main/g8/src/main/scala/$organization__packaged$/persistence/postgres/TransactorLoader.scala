package $organization$.persistence.postgres

import cats.effect._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import $organization$.persistence.postgres.PostgresError.UnhandledPostgresError
import $organization$.util.ExecutionOps
import $organization$.util.error.{ErrorHandle, ErrorRaise}
import $organization$.util.logging.{TraceProvider, TracedLogger}
import $organization$.util.syntax.logging._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import eu.timepit.refined.auto._

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

class TransactorLoader[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider] {

  def createAndVerify(config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      _  <- Resource.liftF(logger.info(log"Loading Postgres module with config \$config"))
      xa <- createFromConfig(config)
      _  <- Resource.liftF(logger.info("Verifying Postgres connection"))
      _  <- Resource.liftF(verifyConnection(config, xa))
    } yield xa

  private def createFromConfig(config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      te <- ExecutionContexts.cachedThreadPool[F]
      xa <- HikariTransactor.newHikariTransactor[F](config.driver, config.uri, config.user, config.password, ce, te)
    } yield xa

  private def verifyConnection(config: PostgresConfig, transactor: HikariTransactor[F]): F[Unit] =
    ExecutionOps.retry(
      name = "Verify Postgres connection",
      fa = verifyConnectionOnce(transactor, config.connectionAttemptTimeout),
      retryPolicy = config.retryPolicy,
      onTimeout = timeoutError(log"Cannot acquire Postgres connection in [\${config.retryPolicy.timeout}]")
    )

  private[postgres] def verifyConnectionOnce(transactor: HikariTransactor[F], timeout: FiniteDuration): F[Unit] = {
    import doobie.implicits._

    val query = sql"""SELECT 1""".query[Int].nel

    val attempt = query
      .transact(transactor)
      .handleErrorWith(e => ErrorRaise[F].raise(UnhandledPostgresError(e)))
      .void

    val timeoutTo = timeoutError[Unit](log"Failed attempt to acquire Postgres connection in [\$timeout]")

    Concurrent.timeoutTo(attempt, timeout, timeoutTo)
  }

  private def timeoutError[A](cause: String): F[A] =
    ErrorRaise[F].raise[A](UnhandledPostgresError(new TimeoutException(cause)))

  private val logger: TracedLogger[F] = TracedLogger.create(getClass)

}

object TransactorLoader {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider] = new TransactorLoader[F]

}
