package $organization$.service

import cats.effect._
import $organization$.persistence.PersistenceModule
import $organization$.service.user.UserService
import $organization$.service.user.api.UserApi
import $organization$.service.user.domain.UserRepository
import $organization$.util.error.{ErrorHandle, ErrorIdGen}
import $organization$.util.logging.TraceProvider
import com.typesafe.config.Config
import io.odin.Logger

class ServiceModuleResource[F[_]: Sync: ErrorHandle: TraceProvider: ErrorIdGen: Logger] {

  def create(rootConfig: Config, persistenceModule: PersistenceModule[F]): Resource[F, ServiceModule[F]] = {
    val _       = rootConfig
    val userApi = new UserApi[F](new UserService[F](new UserRepository[F](persistenceModule.transactor)))
    Resource.pure(ServiceModule[F](userApi))
  }

}
