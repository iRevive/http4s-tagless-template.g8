package $organization$.persistence.mongo

import cats.effect.concurrent.Ref
import cats.mtl.implicits._
import cats.syntax.flatMap._
import $organization$.persistence.mongo.MongoError.UnhandledMongoError
import $organization$.test.BaseSpec
import $organization$.util.RetryPolicy
import $organization$.util.error.ErrorHandle
import eu.timepit.refined.auto._
import org.mongodb.scala.MongoDatabase

import scala.concurrent.duration._

class MongoLoaderSpec extends BaseSpec {

  "MongoLoader" should {

    "return an error" when {

      "connection in unreachable" in EffectAssertion() {
        def mkLoader(counter: Ref[Eff, Int]): MongoLoader[Eff] = new MongoLoader[Eff] {
          override private[mongo] def verifyConnectionOnce(db: MongoDatabase, timeout: FiniteDuration): Eff[Unit] = {
            counter.update(_ + 1) >> super.verifyConnectionOnce(db, timeout)
          }
        }

        val config = MongoConfig(
          "mongodb://localhost:27017/?streamType=netty",
          "test-database",
          5.millis,
          RetryPolicy(5, 30.millis, 1.second)
        )

        for {
          counter <- Ref.of[Eff, Int](0)
          loader = mkLoader(counter)
          result  <- ErrorHandle[Eff].attempt(loader.createAndVerify(config).use(_ => Eff.unit))
          retries <- counter.get
        } yield {
          inside(result.leftValue) {
            case UnhandledMongoError(cause) =>
              cause.getMessage shouldBe "Cannot acquire MongoDB connection in [1 second]"
              retries shouldBe 6
          }
        }
      }

    }

  }

}