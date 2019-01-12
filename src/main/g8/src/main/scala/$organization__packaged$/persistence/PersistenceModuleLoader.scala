package $organization$.persistence

import cats.effect._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.typesafe.config.Config
import eu.timepit.refined.auto._
$if(useMongo.truthy)$
import $organization$.persistence.mongo.MongoConfig
import $organization$.persistence.mongo.MongoError.UnhandledMongoError
import $organization$.util.ExecutionOps
import $organization$.util.syntax.config._
import $organization$.util.error.{ErrorHandle, ErrorRaise}
import $organization$.util.logging.Loggable.InterpolatorOps._
import $organization$.util.logging.{TraceProvider, TracedLogger}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.TimeoutException
import scala.util.control.NonFatal
$endif$

class PersistenceModuleLoader[F[_]: Timer: ContextShift: ErrorHandle: TraceProvider](
    rootConfig: Config
)(implicit F: Concurrent[F]) {

  $if(useMongo.truthy)$
  def loadPersistenceModule(): Resource[F, PersistenceModule] = {
    for {
      mongoDatabase <- loadMongoDatabase()
    } yield PersistenceModule(mongoDatabase)
  }

  private[persistence] def loadMongoDatabase(): Resource[F, MongoDatabase] = {
    for {
      mongoConfig <- Resource.liftF(ErrorRaise.fromEither[F](rootConfig.load[MongoConfig]("application.persistence.mongodb")))
      _           <- Resource.liftF(logger.info(log"Loading mongo module with config \$mongoConfig"))
      db          <- createMongoDatabase(mongoConfig)
    } yield db
  }

  protected def createMongoDatabase(config: MongoConfig): Resource[F, MongoDatabase] = {
    for {
      _      <- Resource.liftF(logger.info(log"Loading MongoDB module with config \$config"))
      client <- Resource.make(F.delay(MongoClient(config.uri)))(c => F.delay(c.close()))
      db     <- Resource.liftF(F.delay(client.getDatabase(config.database).withCodecRegistry(DEFAULT_CODEC_REGISTRY)))
      _      <- Resource.liftF(logger.info("Verifying MongoDB connection"))

      timeoutError = ErrorRaise[F].raise[Unit](
        UnhandledMongoError(
          new TimeoutException(log"Cannot acquire MongoDB connection in [\${config.retryPolicy.timeout}]")
        )
      )

      _ <- Resource.liftF(
        ExecutionOps.retry("Verify MongoDB connection", verifyMongoConnection(db, config), config.retryPolicy, timeoutError)
      )
    } yield db
  }

  private[persistence] def verifyMongoConnection(db: MongoDatabase, config: MongoConfig): F[Unit] = {
    val timeoutTo = ErrorRaise[F].raise[Unit](
      UnhandledMongoError(
        new TimeoutException(log"Failed attempt to acquire MongoDB connection in [\${config.connectionAttemptTimeout}]")
      )
    )

    val flow = IO
      .fromFuture(IO.delay(db.runCommand(BsonDocument("connectionStatus" -> 1)).toFutureOption()))
      .to[F]
      .recoverWith { case NonFatal(e) => ErrorRaise[F].raise(UnhandledMongoError(e)) }
      .flatMap(a => implicitly[ContextShift[F]].shift.map(_ => a))
      .void

    Concurrent.timeoutTo(flow, config.connectionAttemptTimeout, timeoutTo)
  }

  private val logger = TracedLogger.create[F](getClass)
  $else$
  def loadPersistenceModule(): Resource[F, PersistenceModule] = {
    Resource.pure(PersistenceModule())
  }
  $endif$

}
