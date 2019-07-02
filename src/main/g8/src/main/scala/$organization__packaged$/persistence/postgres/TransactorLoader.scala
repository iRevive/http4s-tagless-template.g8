package $organization$.persistence.postgres

import cats.effect._
import cats.effect.syntax.concurrent._
import cats.mtl.syntax.local._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import $organization$.util.error.{ErrorHandle, ErrorRaise, RaisedError}
import $organization$.util.logging.{TraceId, TraceProvider, TracedLogger}
import $organization$.util.syntax.logging._
import $organization$.util.syntax.retry._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import eu.timepit.refined.auto._

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
    verifyConnectionOnce(transactor, config.connectionAttemptTimeout)
      .retryDefault[RaisedError](config.retryPolicy, logger)
      .timeoutTo(
        config.retryPolicy.timeout,
        timeoutError(log"Cannot acquire Postgres connection in [\${config.retryPolicy.timeout}]")
      )
      .local[TraceId](_.subId("verify-postgres-connection"))

  private[postgres] def verifyConnectionOnce(transactor: HikariTransactor[F], timeout: FiniteDuration): F[Unit] = {
    import doobie.implicits._

    val query = sql"""SELECT 1""".query[Int].nel

    val attempt = query
      .transact(transactor)
      .handleErrorWith(e => ErrorRaise[F].raise(RaisedError.postgres(PostgresError.UnavailableConnection(e))))
      .void

    val timeoutTo = timeoutError[Unit](log"Failed attempt to acquire Postgres connection in [\$timeout]")

    Concurrent.timeoutTo(attempt, timeout, timeoutTo)
  }

  private def timeoutError[A](cause: String): F[A] =
    ErrorRaise[F].raise[A](RaisedError.postgres(PostgresError.ConnectionAttemptTimeout(cause)))

  private val logger: TracedLogger[F] = TracedLogger.create(getClass)

}

object TransactorLoader {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider] = new TransactorLoader[F]

}
