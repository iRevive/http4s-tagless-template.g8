package $organization$.service.user.domain

import io.odin.meta.Render

@scalaz.annotation.deriving(Render)
final case class User(username: String, password: String)
