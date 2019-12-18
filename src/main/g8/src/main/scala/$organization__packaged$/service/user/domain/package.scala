package $organization$.service.user

import $organization$.persistence.postgres.Persisted

package object domain {

  type UserId = NewTypes.UserId
  val UserId = NewTypes.UserId

  type PersistedUser = Persisted[UserId, User]

}
