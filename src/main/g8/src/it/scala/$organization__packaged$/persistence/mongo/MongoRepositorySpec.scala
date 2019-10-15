package $organization$.persistence.mongo

import cats.mtl.implicits._
import $organization$.it.ITSpec
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.auto._
import org.mongodb.scala.model.Filters.{equal => eQual}
import org.scalacheck.{Arbitrary, Gen}

class MongoRepositorySpec extends ITSpec {

  import MongoRepositorySpec.Entity

  "MongoRepository" should {

    "persist and retrieve a value" in withApplication() { app =>
      Eff.delay {
        forAll { (collectionName: NonEmptyString, name: String, number: Int) =>
          val repository = new MongoRepository[Eff](app.persistenceModule.mongoDatabase, collectionName)
          val document   = Entity(name, number)

          EffectAssertion() {
            for {
              _      <- repository.insertOne(document)
              result <- repository.findOne[Entity](eQual("name", name))
            } yield {
              result shouldBe Some(document)
            }
          }
        }
      }
    }

  }

  implicit val nonEmptyStringArbitrary: Arbitrary[NonEmptyString] =
    Arbitrary(Gen.listOfN(10, Gen.alphaChar).map(v => NonEmptyString.unsafeFrom(v.mkString)))

  implicit val stringArbitrary: Arbitrary[String] = Arbitrary(Gen.asciiPrintableStr)

}

object MongoRepositorySpec {

  final case class Entity(name: String, number: Int)

}
