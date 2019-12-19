package $organization$.service.user

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import $organization$.service.user.domain.{PersistedUser, User, UserId, UserRepository}
import $organization$.util.error.{ErrorHandle, ErrorIdGen}
import $organization$.util.logging.{TraceProvider, TracedLogger}
import $organization$.util.syntax.logging._
import $organization$.util.syntax.mtl.raise._

class UserService[F[_]: Sync: TraceProvider: ErrorHandle: ErrorIdGen](repository: UserRepository[F]) {

  def findById(userId: UserId): F[PersistedUser] =
    for {
      _       <- logger.info(log"Looking for user id [\$userId]")
      userOpt <- repository.findById(userId)
      _       <- logger.info(log"Found user \$userOpt")
      user    <- userOpt.toRight(UserValidationError.userNotFound(userId)).pureOrRaise
    } yield user

  def findByUsername(username: String): F[Option[PersistedUser]] =
    for {
      _    <- logger.info(log"Looking for user by username [\$username]")
      user <- repository.findByUsername(username)
      _    <- logger.info(log"Found user \$user")
    } yield user

  def create(user: User): F[PersistedUser] =
    for {
      _    <- logger.info(log"Creating user \$user")
      user <- repository.insert(user)
    } yield user

  def delete(userId: UserId): F[Unit] =
    for {
      _ <- logger.info(log"Deleting user [\$userId]")
      _ <- repository.markDeleted(userId)
    } yield ()

  private val logger: TracedLogger[F] = TracedLogger.create(getClass)

}
