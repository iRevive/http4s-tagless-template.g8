package $organization$.persistence.mongo

import cats.effect._
import cats.effect.syntax.bracket._
import cats.effect.syntax.concurrent._
import cats.mtl.syntax.local._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import $organization$.util.error.{ErrorHandle, ErrorRaise, RaisedError}
import $organization$.util.logging.{TraceId, TraceProvider, TracedLogger}
import $organization$.util.syntax.logging._
import $organization$.util.syntax.retry._
import eu.timepit.refined.auto._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.duration._

class MongoLoader[F[_]: Timer: ContextShift: ErrorHandle: TraceProvider](implicit F: Concurrent[F]) {

  def createAndVerify(config: MongoConfig): Resource[F, MongoDatabase] =
    for {
      _  <- Resource.liftF(logger.info(log"Loading mongo module with config \$config"))
      db <- createFromConfig(config)
      _  <- Resource.liftF(logger.info("Verifying MongoDB connection"))
      _  <- Resource.liftF(verifyConnection(db, config))
    } yield db

  def createFromConfig(config: MongoConfig): Resource[F, MongoDatabase] =
    for {
      _      <- Resource.liftF(logger.info(log"Loading MongoDB module with config \$config"))
      client <- Resource.make(F.delay(MongoClient(config.uri)))(c => F.delay(c.close()))
      db     <- Resource.liftF(F.delay(client.getDatabase(config.database).withCodecRegistry(DEFAULT_CODEC_REGISTRY)))
    } yield db

  private def verifyConnection(db: MongoDatabase, config: MongoConfig): F[Unit] =
    verifyConnectionOnce(db, config.connectionAttemptTimeout)
      .retryDefault[RaisedError](config.retryPolicy, logger)
      .timeoutTo(
        config.retryPolicy.timeout,
        timeoutError(log"Cannot acquire MongoDB connection in [\${config.retryPolicy.timeout}]")
      )
      .local[TraceId](_.subId("verify-mongo-connection"))

  private[mongo] def verifyConnectionOnce(db: MongoDatabase, timeout: FiniteDuration): F[Unit] = {
    val timeoutTo = timeoutError[Unit](log"Failed attempt to acquire MongoDB connection in [\$timeout]")

    val attempt = IO
      .fromFuture(IO.delay(db.runCommand(BsonDocument("connectionStatus" -> 1)).toFutureOption()))
      .to[F]
      .handleErrorWith(e => ErrorRaise[F].raise(RaisedError.mongo(MongoError.UnavailableConnection(e))))
      .guarantee(ContextShift[F].shift)
      .void

    Concurrent.timeoutTo(attempt, timeout, timeoutTo)
  }

  private def timeoutError[A](cause: String): F[A] =
    ErrorRaise[F].raise[A](RaisedError.mongo(MongoError.ConnectionAttemptTimeout(cause)))

  private val logger: TracedLogger[F] = TracedLogger.create(getClass)

}

object MongoLoader {

  def default[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider] = new MongoLoader[F]

}
