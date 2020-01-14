package $organization$.persistence.postgres

import cats.effect._
import cats.mtl.syntax.local._
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.functor._
import $organization$.util.error.{ErrorHandle, ErrorIdGen, RaisedError}
import $organization$.util.execution.Retry
import $organization$.util.logging.RenderInstances._
import $organization$.util.logging.{TraceId, TraceProvider}
import $organization$.util.syntax.mtl.raise._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import eu.timepit.refined.auto._
import io.odin.Logger
import io.odin.syntax._
import retry.mtl.syntax.all._

import scala.concurrent.duration._

class TransactorResource[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider: ErrorIdGen: Logger] {

  def createAndVerify(config: PostgresConfig, blocker: Blocker): Resource[F, HikariTransactor[F]] =
    for {
      _  <- Resource.liftF(logger.info(render"Creating Postgres module with config \$config"))
      xa <- createFromConfig(config, blocker)
      _  <- Resource.liftF(logger.info("Verifying Postgres connection"))
      _  <- Resource.liftF(verifyConnection(config, xa))
    } yield xa

  private def createFromConfig(config: PostgresConfig, blocker: Blocker): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- HikariTransactor.newHikariTransactor[F](config.driver, config.uri, config.user, config.password, ce, blocker)
    } yield xa

  private def verifyConnection(config: PostgresConfig, transactor: HikariTransactor[F]): F[Unit] =
    verifyConnectionOnce(transactor, config.connectionAttemptTimeout)
      .retryingOnAllMtlErrors[RaisedError](Retry.makePolicy(config.retryPolicy), Retry.logErrors(logger))
      .local[TraceId](_.child(TraceId.Const("verify-postgres-connection")))

  private[postgres] def verifyConnectionOnce(transactor: HikariTransactor[F], timeout: FiniteDuration): F[Unit] = {
    import doobie.implicits._

    val query = sql"""SELECT 1""".query[Int].nel

    val attempt = query
      .transact(transactor)
      .handleErrorWith(e => PostgresError.unavailableConnection(e).asLeft.pureOrRaise)
      .void

    val timeoutTo = PostgresError
      .connectionAttemptTimeout(render"Failed attempt to acquire Postgres connection in [\$timeout]")
      .asLeft[Unit]
      .pureOrRaise

    Concurrent.timeoutTo(attempt, timeout, timeoutTo)
  }

  private val logger: Logger[F] = Logger[F]

}

object TransactorResource {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider: ErrorIdGen: Logger] =
    new TransactorResource[F]

}
