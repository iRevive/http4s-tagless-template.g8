package $organization$.service

import cats.effect._
import $organization$.persistence.PersistenceModule
import $organization$.service.user.UserRepository
import com.typesafe.config.Config

class ServiceModuleResource[F[_]: Sync] {

  def create(rootConfig: Config, persistenceModule: PersistenceModule[F]): Resource[F, ServiceModule[F]] = {
    val _ = (rootConfig, persistenceModule)
    Resource.pure(ServiceModule[F](new UserRepository(persistenceModule.transactor)))
  }

}
