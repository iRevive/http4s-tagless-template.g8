package $organization$.service.user.domain

import $organization$.it.generators.persistence.*
import $organization$.it.show.persistence.*
import $organization$.it.{AppSuite, QueryChecker}
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyFiniteString
import org.scalacheck.{Arbitrary, Gen}

object UserRepositorySpec extends AppSuite with QueryChecker {

  test("have a correct mapping of byUsername query") { implicit app =>
    forall { (username: Username) =>
      checkQuery(UserRepository.byUsernameQuery(username))
    }
  }

  test("have a correct mapping of markDeletedQuery query") { implicit app =>
    forall { (userId: UserId) =>
      checkQuery(UserRepository.markDeletedQuery(userId))
    }
  }

  test("have a correct mapping of insert query") { implicit app =>
    forall { (user: User) =>
      checkQuery(UserRepository.insertQuery(user))
    }
  }

}
