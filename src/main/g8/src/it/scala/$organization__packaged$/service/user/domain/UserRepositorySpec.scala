package $organization$.service.user.domain

import $organization$.it.{ITSpec, QueryChecker}
import eu.timepit.refined.scalacheck.all._
import org.scalacheck.Arbitrary

class UserRepositorySpec extends ITSpec with QueryChecker {

  "UserRepository" should {

    "have a correct mapping of byUsername query" in withApplication() { _ =>
      forAll { username: String =>
        checkQuery(UserRepository.byUsernameQuery(username))
      }
    }

    "have a correct mapping of markDeletedQuery query" in withApplication() { _ =>
      forAll { userId: UserId =>
        checkQuery(UserRepository.markDeletedQuery(userId))
      }
    }

    "have a correct mapping of insert query" in withApplication() { _ =>
      forAll { user: User =>
        checkQuery(UserRepository.insertQuery(user))
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
