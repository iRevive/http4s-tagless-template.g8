package $organization$.service.user

import $organization$.util.logging.Loggable

@scalaz.deriving(Loggable)
final case class User(username: String, password: String)
