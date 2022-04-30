package $organization$.service

import cats.effect.*
import $organization$.persistence.Persistence
import $organization$.service.user.UserService
import $organization$.service.user.api.UserApi
import $organization$.service.user.domain.UserRepository
import $organization$.util.ConfigSource
import $organization$.util.error.ErrorChannel
import $organization$.util.trace.TraceProvider
import com.typesafe.config.Config
import io.odin.Logger

final case class Services[F[_]](userApi: UserApi[F])

object Services {

  def create[F[_]: Async: ErrorChannel: TraceProvider: Logger](
      config: ConfigSource[F],
      persistence: Persistence[F]
  ): Resource[F, Services[F]] = {
    val _       = config
    val userApi = new UserApi[F](new UserService[F](new UserRepository[F](persistence.transactor)))
    Resource.pure(Services[F](userApi))
  }

}
