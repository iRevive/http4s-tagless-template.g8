package $organization$.service.user.domain

import cats.effect.Sync
import cats.syntax.functor._
import doobie.hikari.HikariTransactor
import doobie.refined.implicits._
import doobie.implicits._
import doobie.util.query.Query0

class UserRepository[F[_]: Sync](transactor: HikariTransactor[F]) {

  def findById(userId: UserId): F[Option[PersistedUser]] =
    UserRepository.byId(userId).option.transact(transactor)

  def findByUsername(username: String): F[Option[PersistedUser]] =
    UserRepository.byUsernameQuery(username).option.transact(transactor)

  def insert(user: User): F[PersistedUser] =
    UserRepository.insertQuery(user).unique.transact(transactor)

  def markDeleted(userId: UserId): F[Unit] =
    UserRepository.markDeletedQuery(userId).run.transact(transactor).void

}

object UserRepository {

  def byId(userId: UserId): Query0[PersistedUser] =
    sql"""
         SELECT id, created_at, updated_at, deleted_at, username, password
         FROM users
         WHERE id = \$userId;
       """.query

  def byUsernameQuery(username: String): Query0[PersistedUser] =
    sql"""
         SELECT id, created_at, updated_at, deleted_at, username, password
         FROM users
         WHERE username = \$username;
       """.query

  def markDeletedQuery(userId: UserId): doobie.Update0 =
    sql"UPDATE users SET deleted_at = now() WHERE id = \$userId AND deleted_at IS NULL".update

  def insertQuery(user: User): Query0[PersistedUser] =
    sql"""
       INSERT INTO users (username, password)
       VALUES (\${user.username}, \${user.password})
       RETURNING id, created_at, updated_at, deleted_at, username, password;
    """.query

}
