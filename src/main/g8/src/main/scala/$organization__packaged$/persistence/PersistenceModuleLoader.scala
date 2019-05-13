package $organization$.persistence

import cats.effect._
import com.typesafe.config.Config
$if(useMongo.truthy)$
import $organization$.persistence.mongo.{MongoConfig, MongoLoader}
import $organization$.util.error.ErrorHandle
import $organization$.util.syntax.config._
import $organization$.util.logging.TraceProvider
import org.mongodb.scala.MongoDatabase
$endif$

class PersistenceModuleLoader[F[_]: Concurrent: Timer: ContextShift: ErrorHandle: TraceProvider](
    $if(useMongo.truthy)$mongoLoader: MongoLoader[F]$endif$
) {

  $if(useMongo.truthy)$
  def load(rootConfig: Config): Resource[F, PersistenceModule] =
    for {
      mongoDatabase <- loadMongoDatabase(rootConfig)
    } yield PersistenceModule(mongoDatabase)

  private def loadMongoDatabase(rootConfig: Config): Resource[F, MongoDatabase] =
    for {
      mongoConfig <- Resource.liftF(rootConfig.loadF[F, MongoConfig]("application.persistence.mongodb"))
      db          <- mongoLoader.createAndVerify(mongoConfig)
    } yield db

  $else$
  def load: Resource[F, PersistenceModule] = {
    Resource.pure(PersistenceModule())
  }
  $endif$

}
