package $organization$.service.user.api

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import $organization$.service.user.UserService
import $organization$.service.user.domain.{Password, PersistedUser, User, UserId, Username}
import $organization$.util.api.ApiResponse
import $organization$.util.error.ErrorChannel
import $organization$.util.instances.circe.*
import $organization$.util.syntax.json.*
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyFiniteString
import io.circe.refined.*
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder, Json}
import org.http4s.HttpRoutes
import org.http4s.circe.{jsonDecoder, jsonEncoder}
import org.http4s.dsl.Http4sDsl

import scala.util.Try

class UserApi[F[_]: Async: ErrorChannel](val service: UserService[F]) extends Http4sDsl[F] {

  lazy val routes: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root / "api" / "user" / "find" / UsernameVar(username) =>
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

final case class UserView(id: UserId, username: Username) derives Codec.AsObject

final case class CreateUser(username: Username, password: Password) derives Codec.AsObject

object UserIdVar {

  def unapply(str: String): Option[UserId] =
    for {
      number <- Try(str.toInt).toOption
      id     <- PosInt.from(number).toOption
    } yield UserId(id)

}

object UsernameVar {

  def unapply(str: String): Option[Username] =
    for {
      username <- NonEmptyFiniteString[50].from(str).toOption
    } yield Username(username)

}
