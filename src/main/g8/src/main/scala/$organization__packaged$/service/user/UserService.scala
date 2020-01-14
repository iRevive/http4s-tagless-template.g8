package $organization$.service.user

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import $organization$.service.user.domain.{PersistedUser, User, UserId, UserRepository}
import $organization$.util.error.{ErrorHandle, ErrorIdGen}
import $organization$.util.syntax.mtl.raise._
import io.odin.Logger
import io.odin.syntax._

class UserService[F[_]: Sync: ErrorHandle: ErrorIdGen: Logger](repository: UserRepository[F]) {

  def findById(userId: UserId): F[PersistedUser] =
    for {
      _       <- logger.info(render"Looking for user id [\$userId]")
      userOpt <- repository.findById(userId)
      _       <- logger.info(render"Found user \$userOpt")
      user    <- userOpt.toRight(UserValidationError.userNotFound(userId)).pureOrRaise
    } yield user

  def findByUsername(username: String): F[Option[PersistedUser]] =
    for {
      _    <- logger.info(render"Looking for user by username [\$username]")
      user <- repository.findByUsername(username)
      _    <- logger.info(render"Found user \$user")
    } yield user

  def create(user: User): F[PersistedUser] =
    for {
      _    <- logger.info(render"Creating user \$user")
      user <- repository.insert(user)
    } yield user

  def delete(userId: UserId): F[Unit] =
    for {
      _ <- logger.info(render"Deleting user [\$userId]")
      _ <- repository.markDeleted(userId)
    } yield ()

  private val logger: Logger[F] = Logger[F]

}
