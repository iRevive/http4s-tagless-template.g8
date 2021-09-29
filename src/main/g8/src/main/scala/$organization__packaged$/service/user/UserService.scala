package $organization$.service.user

import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import $organization$.service.user.domain.{PersistedUser, User, UserId, UserRepository, Username}
import $organization$.util.error.ErrorChannel
import io.odin.Logger
import io.odin.syntax.*

class UserService[F[_]: Sync: ErrorChannel: Logger](repository: UserRepository[F]) {

  def findById(userId: UserId): F[PersistedUser] =
    for {
      _       <- logger.info(render"Looking for user id [\$userId]")
      userOpt <- repository.findById(userId)
      _       <- logger.info(render"Found user \$userOpt")
      user    <- ErrorChannel[F].raiseEither(userOpt.toRight(UserValidationError.UserNotFound(userId)))
    } yield user

  def findByUsername(username: Username): F[Option[PersistedUser]] =
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
