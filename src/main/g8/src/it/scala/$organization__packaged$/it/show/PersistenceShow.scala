package $organization$.it.show

import cats.Show
import $organization$.service.user.domain.User
import io.odin.meta.Render

private[show] trait PersistenceShow {
  implicit val userShow: Show[User] = Show.show(Render[User].render)
}
