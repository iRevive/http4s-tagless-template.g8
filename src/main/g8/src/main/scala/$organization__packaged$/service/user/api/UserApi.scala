package $organization$.service.user.api

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import $organization$.service.user.UserService
import $organization$.service.user.domain.{PersistedUser, User, UserId}
import $organization$.util.api.ApiResponse
import $organization$.util.error.{ErrorIdGen, ErrorRaise}
import $organization$.util.syntax.json._
import $organization$.util.json.JsonCodecs._
import eu.timepit.refined.types.numeric.PosInt
import io.circe.refined._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.http4s.HttpRoutes
import org.http4s.circe.{jsonDecoder, jsonEncoder}
import org.http4s.dsl.Http4sDsl

import scala.util.Try

class UserApi[F[_]: Sync: ErrorRaise: ErrorIdGen](val service: UserService[F]) extends Http4sDsl[F] {

  lazy val routes: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root / "api" / "user" / "find" / username =>
      for {
        user     <- service.findByUsername(username)
        response <- Ok(ApiResponse.Success(user.map(toView)).asJson)
      } yield response

    case GET -> Root / "api" / "user" / UserIdVar(userId) =>
      for {
        user     <- service.findById(userId)
        response <- Ok(ApiResponse.Success(toView(user)).asJson)
      } yield response

    case req @ POST -> Root / "api" / "user" =>
      for {
        json     <- req.as[Json]
        entity   <- json.decodeF[F, CreateUser]
        user     <- service.create(User(entity.username, entity.password))
        response <- Created(ApiResponse.Success(toView(user)).asJson)
      } yield response

    case DELETE -> Root / "api" / "user" / UserIdVar(userId) =>
      service.delete(userId) >> NoContent()
  }

  private def toView(user: PersistedUser): UserView =
    UserView(user.id, user.entity.username)

}

@scalaz.annotation.deriving(Decoder, Encoder)
final case class UserView(id: UserId, username: String)

@scalaz.annotation.deriving(Decoder, Encoder)
final case class CreateUser(username: String, password: String)

object UserIdVar {

  def unapply(str: String): Option[UserId] =
    for {
      number <- Try(str.toInt).toOption
      id     <- PosInt.from(number).toOption
    } yield UserId(id)

}
