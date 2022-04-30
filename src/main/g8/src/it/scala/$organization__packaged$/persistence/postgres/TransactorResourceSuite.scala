package $organization$.persistence.postgres

import cats.effect.{Ref, Resource}
import cats.mtl.implicits.*
import cats.syntax.applicativeError.*
import cats.syntax.flatMap.*
import $organization$.it.AppSuite
import $organization$.persistence.Persistence
import $organization$.persistence.postgres.PostgresConfig
import $organization$.util.ConfigSource
import $organization$.util.error.AppError.select
import $organization$.util.error.{ErrorChannel, ErrorIdGen, RaisedError}
import $organization$.util.execution.Retry
import $organization$.util.logging.Loggers
import com.typesafe.config.{ConfigFactory, ConfigOriginFactory}
import doobie.hikari.HikariTransactor
import io.odin.{Level, Logger}
import pureconfig.error.*
import pureconfig.ConfigSource as ConfigSrc

import scala.concurrent.duration.*

object TransactorResourceSuite extends AppSuite {

  test("return an error when config is missing") {
    val config = ConfigFactory.parseString(
      """
        |application.persistence {
        |
        |}
      """.stripMargin
    )

    val cfg = new ConfigSource[Eff](ConfigSrc.fromConfig(config))
    val expected = ConfigReaderException[PostgresConfig](
      ConfigReaderFailures(
        ConvertFailure(
          KeyNotFound("postgres"),
          Some(ConfigOriginFactory.newSimple("String").withLineNumber(2)),
          "application.persistence"
        )
      )
    )

    for {
      result <- TransactorResource.create(cfg).use_.attempt
    } yield expect(result == Left(expected))
  }

  test("return an error when config is missing") {
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

    val cfg = new ConfigSource[Eff](ConfigSrc.fromConfig(config))
    val expected = ConfigReaderException[PostgresConfig](
      ConfigReaderFailures(
        ConvertFailure(
          KeyNotFound("delay"),
          None,
          "application.persistence.postgres.retry-policy.delay"
        )
      )
    )

    for {
      result <- TransactorResource.create(cfg).use_.attempt
    } yield expect(result == Left(expected))
  }

  test("return an error when connection in unreachable") {
    def mkResource(counter: Ref[Eff, Int]): TransactorResource[Eff] =
      new TransactorResource[Eff] {
        override private[postgres] def verifyConnectionOnce(
            transactor: HikariTransactor[Eff],
            timeout: FiniteDuration
        ): Eff[Unit] = counter.update(_ + 1) >> super.verifyConnectionOnce(transactor, timeout)
      }

    val config = PostgresConfig(
      driver = "org.postgresql.Driver",
      uri = "jdbc:postgresql://localhost:5432/postgres",
      user = "root",
      password = "root",
      connectionAttemptTimeout = 5.millis,
      runMigration = false,
      retryPolicy = Retry.Policy(5, 30.millis, 5.second)
    )

    for {
      counter <- Ref.of[Eff, Int](0)
      result  <- mkResource(counter).createAndVerify(config).use_.attemptHandle[RaisedError]
      retries <- counter.get
    } yield expect.all(result.left.toOption.flatMap(_.error.select[PostgresError]).isDefined, retries == 6)
  }

  test("create a transactor") {
    for {
      config <- ConfigSource.fromTypesafeConfig[Eff]
      result <- TransactorResource.create(config).use_.attempt
    } yield expect(result.isRight)
  }

  private implicit val logger: Logger[Eff]             = Loggers.consoleContextLogger(Level.Info)
  private implicit val errorChannel: ErrorChannel[Eff] = ErrorChannel.create(ErrorIdGen.const("test"))

}
