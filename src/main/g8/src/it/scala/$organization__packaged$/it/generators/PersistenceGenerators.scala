package $organization$.it.generators

import $organization$.it.generators.common.*
import $organization$.service.user.domain.{Password, User, UserId, Username}
import eu.timepit.refined.scalacheck.all.*
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyFiniteString
import org.scalacheck.{Arbitrary, Gen}

private[generators] trait PersistenceGenerators {

  implicit val userIdArbitrary: Arbitrary[UserId] =
    Arbitrary(Arbitrary.arbitrary[PosInt].map(UserId.apply))

  implicit val usernameArbitrary: Arbitrary[Username] =
    Arbitrary(Gen.uuid.map(uuid => Username(NonEmptyFiniteString[50].unsafeFrom(uuid.toString))))

  implicit val passwordArbitrary: Arbitrary[Password] =
    Arbitrary(Arbitrary.arbitrary[NonEmptyFiniteString[50]].map(Password.apply))

  implicit val userArbitrary: Arbitrary[User] =
    Arbitrary {
      for {
        username <- Arbitrary.arbitrary[Username]
        password <- Arbitrary.arbitrary[Password]
      } yield User(username, password)
    }

}
