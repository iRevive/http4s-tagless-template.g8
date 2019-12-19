package $organization$.service.user.domain

import $organization$.util.logging.Loggable

@scalaz.annotation.deriving(Loggable)
final case class User(username: String, password: String)
