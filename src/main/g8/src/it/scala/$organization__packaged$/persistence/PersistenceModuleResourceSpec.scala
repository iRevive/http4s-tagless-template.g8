package $organization$.persistence

import cats.effect.Blocker
import cats.mtl.implicits._
import $organization$.it.ITSpec
import $organization$.util.config.ConfigParsingError
import $organization$.util.error.RaisedError
import com.typesafe.config.ConfigFactory
import io.odin.Logger

class PersistenceModuleResourceSpec extends ITSpec {

  "PersistenceModuleResource" when {

    "creating Transactor" should {

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
            transactor <- moduleResource.createTransactor(config, blocker)
          } yield transactor

          for {
            result <- fa.use(_ => Eff.unit).attemptHandle[RaisedError]
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
            transactor <- moduleResource.createTransactor(config, blocker)
          } yield transactor

          for {
            result <- fa.use(_ => Eff.unit).attemptHandle[RaisedError]
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

      "create transactor" in EffectAssertion() {
        val fa = for {
          blocker    <- Blocker[Eff]
          transactor <- moduleResource.createTransactor(DefaultConfig, blocker)
        } yield transactor

        for {
          result <- fa.use(_ => Eff.unit).attemptHandle[RaisedError]
        } yield {
          result should beRight(())
        }
      }

    }

  }

  private lazy val moduleResource = {
    implicit val logger: Logger[Eff] = io.odin.consoleLogger[Eff]()

    PersistenceModuleResource.default[Eff]
  }

}
