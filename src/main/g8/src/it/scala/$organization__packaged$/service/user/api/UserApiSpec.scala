package $organization$.service.user.api

import cats.syntax.applicative._
import $organization$.it.ApiSpec
import $organization$.service.user.domain.{User, UserId}
import $organization$.util.api.ApiResponse
import eu.timepit.refined.types.numeric.PosInt
import io.circe.syntax._
import io.odin.syntax._
import org.http4s.circe.jsonEncoder
import org.http4s.syntax.literals._
import org.http4s.{Method, Request, Status}
import org.scalacheck.{Arbitrary, Gen}

class UserApiSpec extends ApiSpec {

  "UserApi" when afterWord("endpoint is") {

    "GET /api/user/find/{username}" should {

      behave like authorizedApi(Method.GET, uri"/api/user/find/username")

      "return empty response" when {

        "user with specified username does not exist" in withApplication() { implicit app =>
          forAll(nonEmptyString) { username =>
            val request      = withAuth(Request[Eff](Method.GET, uri"/api/user/find" / username))
            val expectedBody = ApiResponse.Success(Option.empty[UserView])

            executeAndCheck[ApiResponse.Success[Option[UserView]]](request, Status.Ok, expectedBody)
          }
        }

      }

      "return user" in withApplication() { implicit app =>
        forAll(nonEmptyString, nonEmptyString) { (username, password) =>
          val request = withAuth(Request[Eff](Method.GET, uri"/api/user/find" / username))

          for {
            user         <- app.serviceModule.userApi.service.create(User(username, password))
            expectedBody <- ApiResponse.Success(Option(UserView(user.id, username))).pure[Eff]
            _            <- executeAndCheck[ApiResponse.Success[Option[UserView]]](request, Status.Ok, expectedBody)
          } yield ()
        }
      }

    }

    "GET /api/user/{userId}" should {

      behave like authorizedApi(Method.GET, uri"/api/user/123")

      "return NotFound" when {

        "user with specified userId does not exist" in withApplication() { implicit app =>
          forAll { userId: UserId =>
            val request      = withAuth(Request[Eff](Method.GET, uri"/api/user" / userId.toString))
            val expectedBody = ApiResponse.Error(render"User with userId [\$userId] does not exist", "test")

            executeAndCheck[ApiResponse.Error](request, Status.NotFound, expectedBody)
          }
        }

      }

      "return user" in withApplication() { implicit app =>
        forAll(nonEmptyString, nonEmptyString) { (username, password) =>
          for {
            user         <- app.serviceModule.userApi.service.create(User(username, password))
            request      <- withAuth(Request[Eff](Method.GET, uri"/api/user" / user.id.toString)).pure[Eff]
            expectedBody <- ApiResponse.Success(UserView(user.id, username)).pure[Eff]
            _            <- executeAndCheck[ApiResponse.Success[UserView]](request, Status.Ok, expectedBody)
          } yield ()
        }
      }

    }

    "POST /api/user" should {

      behave like authorizedApi(Method.POST, uri"/api/user")
      behave like metaResponseForInvalidJson(Method.POST, uri"/api/user")

      "create user" in withApplication() { implicit app =>
        forAll(nonEmptyString, nonEmptyString) { (username, password) =>
          val body    = CreateUser(username, password)
          val request = withAuth(Request[Eff](Method.POST, uri"/api/user").withEntity(body.asJson))

          for {
            (response, body) <- executeRequest[ApiResponse.Success[UserView]](request)
            user             <- app.serviceModule.userApi.service.findById(body.result.id)
          } yield {
            response.status shouldBe Status.Created
            user.entity shouldBe User(username, password)
          }
        }
      }

    }

    "DELETE /api/user/{userId}" should {

      behave like authorizedApi(Method.DELETE, uri"/api/user/123")

      "delete user" in withApplication() { implicit app =>
        forAll(nonEmptyString, nonEmptyString) { (username, password) =>
          for {
            user     <- app.serviceModule.userApi.service.create(User(username, password))
            request  <- withAuth(Request[Eff](Method.DELETE, uri"/api/user" / user.id.toString)).pure[Eff]
            response <- app.apiModule.httpApp.run(request)
            deleted  <- app.serviceModule.userApi.service.findById(user.id)
          } yield {
            response.status shouldBe Status.NoContent
            deleted.deletedAt should not be empty
          }
        }
      }

    }

  }

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration().copy(minSuccessful = 1, sizeRange = 5)

  private implicit val userIdArbitrary: Arbitrary[UserId] = {
    import eu.timepit.refined.auto._
    import eu.timepit.refined.scalacheck.numeric._

    Arbitrary(Arbitrary.arbitrary[PosInt].filter(_ > 10000).map(UserId.apply))
  }

  private val nonEmptyString: Gen[String] =
    Gen.alphaNumStr.filter(_.trim.nonEmpty)

}
