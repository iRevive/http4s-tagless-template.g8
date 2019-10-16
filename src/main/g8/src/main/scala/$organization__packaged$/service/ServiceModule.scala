package $organization$.service

import $organization$.service.user.UserRepository

final case class ServiceModule[F[_]](userRepository: UserRepository[F])
