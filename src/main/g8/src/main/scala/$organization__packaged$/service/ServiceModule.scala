package $organization$.service

import $organization$.service.user.api.UserApi

final case class ServiceModule[F[_]](userApi: UserApi[F])
