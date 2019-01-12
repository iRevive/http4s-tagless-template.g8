package $organization$.persistence.mongo

import cats.mtl.implicits._
import $organization$.it.ITSpec
import io.circe.generic.auto._
import org.mongodb.scala.model.Filters.{equal => eQual}

import scala.util.Random

class MongoRepositorySpec extends ITSpec {

  import MongoRepositorySpec.Entity

  "MongoRepository" should {

    "persist and retrieve a value" in withApplication() { app =>
      val collectionName = randomNonEmptyString()
      val repository     = new MongoRepository[Eff, Entity](app.persistenceModule.mongoDatabase, collectionName)

      val name   = randomString()
      val number = Random.nextInt()

      val document = Entity(name, number)

      for {
        _      <- repository.insertOne(document)
        result <- repository.findOne(eQual("name", name))
      } yield {
        result shouldBe document
      }
    }

  }

}

object MongoRepositorySpec {

  final case class Entity(name: String, number: Int)

}
