package $organization$.processing

import cats.effect._
import $organization$.persistence.PersistenceModule
import com.typesafe.config.Config

class ProcessingModuleLoader[F[_]: Sync](rootConfig: Config, persistenceModule: PersistenceModule) {

  def loadProcessingModule(): Resource[F, ProcessingModule] = {
    Resource.pure(ProcessingModule())
  }

}
