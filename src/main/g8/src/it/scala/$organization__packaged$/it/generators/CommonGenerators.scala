package $organization$.it.generators

import eu.timepit.refined.auto.*
import eu.timepit.refined.scalacheck.all.*
import eu.timepit.refined.types.string.NonEmptyFiniteString
import org.scalacheck.{Arbitrary, Gen}

private[generators] trait CommonGenerators {

  implicit val nonEmptyFiniteStringArbitrary: Arbitrary[NonEmptyFiniteString[50]] = {
    implicit val charArb: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
    Arbitrary(Arbitrary.arbitrary[NonEmptyFiniteString[50]])
  }

}
