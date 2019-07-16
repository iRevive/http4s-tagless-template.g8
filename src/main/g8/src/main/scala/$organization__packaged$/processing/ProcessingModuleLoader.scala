package $organization$.processing

import cats.effect._
import $organization$.persistence.PersistenceModule
import com.typesafe.config.Config

class ProcessingModuleLoader[F[_]: Sync] {

  def load(rootConfig: Config, persistenceModule: PersistenceModule[F]): Resource[F, ProcessingModule[F]] = {
    val _ = (rootConfig, persistenceModule)
    Resource.pure(ProcessingModule[F]())
  }

}
