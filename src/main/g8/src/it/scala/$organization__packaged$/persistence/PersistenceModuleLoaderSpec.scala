package $organization$.persistence

$if(useMongo.truthy)$
import cats.mtl.implicits._
import com.typesafe.config.ConfigFactory
import $organization$.persistence.mongo.MongoLoader
import $organization$.it.ITSpec
import $organization$.util.config.ConfigParsingError
import $organization$.util.error.ErrorHandle
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

          val loader = new PersistenceModuleLoader[Eff](MongoLoader.default)

          for {
            result <- ErrorHandle[Eff].attempt(loader.load(config).use(_ => Eff.unit))
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
              |  database = "test_database"
              |  connection-attempt-timeout = 500 milliseconds
              |  retry-policy {
              |    retries = 10
              |    timeout = 60 seconds
              |  }
              |}
            """.stripMargin
          )

          val loader = new PersistenceModuleLoader[Eff](MongoLoader.default)

          for {
            result <- ErrorHandle[Eff].attempt(loader.load(config).use(_ => Eff.unit))
          } yield {
            inside(result.leftValue) {
              case ConfigParsingError(path, expectedClass, err) =>
                path shouldBe "application.persistence.mongodb"
                expectedClass shouldBe "MongoConfig"
                err.getMessage shouldBe "Attempt to decode value on failed cursor: DownField(delay),DownField(retry-policy)"
            }
          }
        }

      }

    }
    $endif$

  }

}
