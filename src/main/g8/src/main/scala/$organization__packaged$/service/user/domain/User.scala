package $organization$.service.user.domain

import io.odin.extras.derivation.render.derived
import io.odin.meta.Render

final case class User(username: Username, password: Password) derives Render
