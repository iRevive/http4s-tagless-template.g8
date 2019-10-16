package $organization$.service

import $organization$.persistence.postgres.Persisted

package object user {

  type UserId = NewTypes.UserId
  val UserId = NewTypes.UserId

  type PersistedUser = Persisted[UserId, User]

}
