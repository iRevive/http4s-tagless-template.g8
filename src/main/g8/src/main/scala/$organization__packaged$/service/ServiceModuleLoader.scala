package $organization$.service

import cats.effect._
import $organization$.persistence.PersistenceModule
import $organization$.service.user.UserRepository
import com.typesafe.config.Config

class ServiceModuleLoader[F[_]: Sync] {

  def load(rootConfig: Config, persistenceModule: PersistenceModule[F]): Resource[F, ServiceModule[F]] = {
    val _ = (rootConfig, persistenceModule)
    Resource.pure(ServiceModule[F](new UserRepository(persistenceModule.transactor)))
  }

}
