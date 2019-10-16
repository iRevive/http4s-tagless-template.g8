package $organization$.persistence

import cats.effect.Blocker
import cats.mtl.implicits._
import $organization$.it.ITSpec
import $organization$.util.config.ConfigParsingError
import $organization$.util.error.ErrorHandle
import com.typesafe.config.ConfigFactory

class PersistenceModuleLoaderSpec extends ITSpec {

  "PersistenceModuleLoader" when {

    "loading Transactor" should {

      "return an error" when {

        "config is missing" in EffectAssertion() {
          val config = ConfigFactory.parseString(
            """
              |application.persistence {
              |
              |}
            """.stripMargin
          )

          val fa = for {
            blocker    <- Blocker[Eff]
            transactor <- loader.loadTransactor(config, blocker)
          } yield transactor

          for {
            result <- ErrorHandle[Eff].attempt(fa.use(_ => Eff.unit))
          } yield {
            inside(result.leftValue.error.select[ConfigParsingError].value) {
              case ConfigParsingError(path, expectedClass, err) =>
                path shouldBe "application.persistence.postgres"
                expectedClass shouldBe "PostgresConfig"
                err.getMessage shouldBe "Path not found in config"
            }
          }
        }

        "config is invalid" in EffectAssertion() {
          val config = ConfigFactory.parseString(
            """
              |application.persistence.postgres {
              |  driver = "org.postgresql.Driver"
              |  uri = "jdbc:postgresql://localhost:5432/postgres"
              |  user = "root"
              |  password = "root"
              |  connection-attempt-timeout = 500 milliseconds
              |  run-migration = false
              |  retry-policy {
              |    retries = 10
              |    timeout = 60 seconds
              |  }
              |}
            """.stripMargin
          )

          val fa = for {
            blocker    <- Blocker[Eff]
            transactor <- loader.loadTransactor(config, blocker)
          } yield transactor

          for {
            result <- ErrorHandle[Eff].attempt(fa.use(_ => Eff.unit))
          } yield {
            inside(result.leftValue.error.select[ConfigParsingError].value) {
              case ConfigParsingError(path, expectedClass, err) =>
                path shouldBe "application.persistence.postgres"
                expectedClass shouldBe "PostgresConfig"
                err.getMessage shouldBe "Attempt to decode value on failed cursor: DownField(delay),DownField(retry-policy)"
            }
          }
        }

      }

      "load transactor" in EffectAssertion() {
        val fa = for {
          blocker    <- Blocker[Eff]
          transactor <- loader.loadTransactor(DefaultConfig, blocker)
        } yield transactor

        for {
          result <- ErrorHandle[Eff].attempt(fa.use(_ => Eff.unit))
        } yield {
          result should beRight(())
        }
      }

    }

  }

  private val loader = PersistenceModuleLoader.default[Eff]

}
