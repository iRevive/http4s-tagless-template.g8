package $organization$.persistence

$if(useMongo.truthy)$
import cats.mtl.implicits._
import com.typesafe.config.ConfigFactory
import $organization$.persistence.mongo.MongoConfig
import $organization$.persistence.mongo.MongoError.UnhandledMongoError
import $organization$.it.ITSpec
import $organization$.util.config.ConfigParsingError
import org.mongodb.scala.MongoDatabase
$else$
import $organization$.it.ITSpec
$endif$


class PersistenceModuleLoaderSpec extends ITSpec {

  "PersistenceModuleLoader" when {

    $if(useMongo.truthy)$
    "loading Mongo module" should {

      "return an error" when {

        "config is missing" in EffectAssertion() {
          val config = ConfigFactory.parseString(
            """
              |application.persistence {
              |
              |}
            """.stripMargin
          )

          val loader = new PersistenceModuleLoader[Eff](config)

          for {
            result <- loader.loadMongoDatabase().use(_ => Eff.unit).run(traceId).value
          } yield {
            inside(result.leftValue) {
              case ConfigParsingError(path, expectedClass, err) =>
                path shouldBe "application.persistence.mongodb"
                expectedClass shouldBe "MongoConfig"
                err.getMessage shouldBe "Path not found in config"
            }
          }
        }

        "config is invalid" in EffectAssertion() {
          val config = ConfigFactory.parseString(
            """
              |application.persistence.mongodb {
              |  uri = "mongodb://localhost:27017/?streamType=netty"
              |  database = "macrotest"
              |  connection-attempt-timeout = 500 milliseconds
              |  retry-policy {
              |    retries = 10
              |    timeout = 60 seconds
              |  }
              |}
            """.stripMargin
          )

          val loader = new PersistenceModuleLoader[Eff](config)

          for {
            result <- loader.loadMongoDatabase().use(_ => Eff.unit).run(traceId).value
          } yield {
            inside(result.leftValue) {
              case ConfigParsingError(path, expectedClass, err) =>
                path shouldBe "application.persistence.mongodb"
                expectedClass shouldBe "MongoConfig"
                err.getMessage shouldBe "Attempt to decode value on failed cursor: DownField(delay),DownField(retry-policy)"
            }
          }
        }

        "connection in unreachable" in EffectAssertion() {
          val config = ConfigFactory.parseString(
            """
              |application.persistence.mongodb {
              |  uri = "mongodb://localhost:27017/?streamType=netty"
              |  database = "macrotest"
              |  connection-attempt-timeout = 5 milliseconds
              |  retry-policy {
              |    retries = 5
              |    delay = 30 milliseconds
              |    timeout = 1000 milliseconds
              |  }
              |}
            """.stripMargin
          )

          var counter = 0

          val loader = new PersistenceModuleLoader[Eff](config) {
            override private[persistence] def verifyMongoConnection(db: MongoDatabase, config: MongoConfig): Eff[Unit] = {
              for {
                _ <- Eff.delay { counter = counter + 1 }
                _ <- super.verifyMongoConnection(db, config)
              } yield ()
            }
          }

          for {
            result <- loader.loadMongoDatabase().use(_ => Eff.unit).run(traceId).value
          } yield {
            inside(result.leftValue) {
              case UnhandledMongoError(cause) =>
                cause.getMessage shouldBe "Cannot acquire MongoDB connection in [1 second]"

                counter shouldBe 6
            }
          }
        }

      }

    }
    $endif$

  }

}
