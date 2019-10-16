package $organization$.service.user

import $organization$.it.{ITSpec, QueryChecker}
import eu.timepit.refined.scalacheck.all._
import org.scalacheck.Arbitrary

class UserRepositorySpec extends ITSpec with QueryChecker {

  "UserRepository" should {

    "have a correct mapping of byUsername query" in withApplication() { _ =>
      Eff.delay {
        forAll { username: String =>
          check(UserRepository.byUsernameQuery(username))
        }
      }
    }

    "have a correct mapping of markDeletedQuery query" in withApplication() { _ =>
      Eff.delay {
        forAll { userId: UserId =>
          check(UserRepository.markDeletedQuery(userId))
        }
      }
    }

    "have a correct mapping of insert query" in withApplication() { _ =>
      Eff.delay {
        forAll { user: User =>
          check(UserRepository.insertQuery(user))
        }
      }
    }
  }

  private implicit val userArbitrary: Arbitrary[User] =
    Arbitrary {
      for {
        username <- Arbitrary.arbitrary[String]
        password <- Arbitrary.arbitrary[String]
      } yield User(username, password)
    }

}
