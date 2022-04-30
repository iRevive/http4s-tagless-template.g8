package $organization$.service.user.api

import cats.syntax.applicative.*
import $organization$.it.ApiSuite
import $organization$.it.generators.persistence.*
import $organization$.service.user.domain.{Password, User, UserId, Username}
import $organization$.util.api.ApiResponse
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyFiniteString
import io.circe.syntax.*
import io.odin.syntax.*
import org.http4s.circe.jsonEncoder
import org.http4s.syntax.literals.*
import org.http4s.{Method, Request, Status}
import org.scalacheck.{Arbitrary, Gen}

object UserApiSuite extends ApiSuite {

  // Find
  authorizedApi(Method.GET, uri"/api/user/find/username")

  test("return empty response when user with specified username does not exist") { implicit app =>
    forall { (username: Username) =>
      val request      = withAuth(Request[Eff](Method.GET, uri"/api/user/find" / username.value))
      val expectedBody = ApiResponse.Success(None): ApiResponse.Success[Option[UserView]]

      executeAndCheck[ApiResponse.Success[Option[UserView]]](request, Status.Ok, expectedBody)
    }
  }

  test("return user by find") { implicit app =>
    forall { (username: Username, password: Password) =>
      val request = withAuth(Request[Eff](Method.GET, uri"/api/user/find" / username.value))

      for {
        user         <- app.services.userApi.service.create(User(username, password))
        expectedBody <- ApiResponse.Success(Option(UserView(user.id, username))).pure[Eff]
        result       <- executeAndCheck[ApiResponse.Success[Option[UserView]]](request, Status.Ok, expectedBody)
      } yield result
    }
  }

  // Get by id
  authorizedApi(Method.GET, uri"/api/user/123")

  test("user with specified userId does not exist") { implicit app =>
    forall { (userId: UserId) =>
      val request      = withAuth(Request[Eff](Method.GET, uri"/api/user" / userId.toString))
      val expectedBody = ApiResponse.Error(render"User with userId [\$userId] does not exist", "test"): ApiResponse.Error

      executeAndCheck[ApiResponse.Error](request, Status.NotFound, expectedBody)
    }
  }

  test("return user by id") { implicit app =>
    forall { (username: Username, password: Password) =>
      for {
        user         <- app.services.userApi.service.create(User(username, password))
        request      <- withAuth(Request[Eff](Method.GET, uri"/api/user" / user.id.toString)).pure[Eff]
        expectedBody <- ApiResponse.Success(UserView(user.id, username)).pure[Eff]
        result       <- executeAndCheck[ApiResponse.Success[UserView]](request, Status.Ok, expectedBody)
      } yield result
    }
  }

  // Create
  authorizedApi(Method.POST, uri"/api/user")
  metaResponseForInvalidJson(Method.POST, uri"/api/user")

  test("create user") { implicit app =>
    forall { (username: Username, password: Password) =>
      val body    = CreateUser(username, password)
      val request = withAuth(Request[Eff](Method.POST, uri"/api/user").withEntity(body.asJson))

      for {
        (response, body) <- executeRequest[ApiResponse.Success[UserView]](request)
        user             <- app.services.userApi.service.findById(body.result.id)
      } yield expect.all(response.status == Status.Created, user.entity == User(username, password))
    }
  }

  // Delete
  authorizedApi(Method.DELETE, uri"/api/user/123")

  test("delete user") { implicit app =>
    forall { (username: Username, password: Password) =>
      for {
        user     <- app.services.userApi.service.create(User(username, password))
        request  <- withAuth(Request[Eff](Method.DELETE, uri"/api/user" / user.id.toString)).pure[Eff]
        response <- app.api.httpApp.run(request)
        deleted  <- app.services.userApi.service.findById(user.id)
      } yield expect.all(response.status == Status.NoContent, deleted.deletedAt.nonEmpty)
    }
  }

}
