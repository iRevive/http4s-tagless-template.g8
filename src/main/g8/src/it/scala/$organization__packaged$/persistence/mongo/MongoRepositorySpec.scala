package $organization$.persistence.mongo

import cats.mtl.implicits._
import $organization$.it.ITSpec
import eu.timepit.refined.scalacheck.string._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.auto._
import org.mongodb.scala.model.Filters.{equal => eQual}
import org.scalacheck.{Arbitrary, Gen}

import scala.util.Random

class MongoRepositorySpec extends ITSpec {

  import MongoRepositorySpec.Entity

  "MongoRepository" should {

    "persist and retrieve a value" in withApplication() { app =>
      implicit val stringArb: Arbitrary[String] = Arbitrary(Gen.asciiPrintableStr)

      forAll { (collectionName: NonEmptyString, name: String, number: Int) =>
        val repository = new MongoRepository[Eff](app.persistenceModule.mongoDatabase, collectionName)
        val document   = Entity(name, number)

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

object MongoRepositorySpec {

  final case class Entity(name: String, number: Int)

}
