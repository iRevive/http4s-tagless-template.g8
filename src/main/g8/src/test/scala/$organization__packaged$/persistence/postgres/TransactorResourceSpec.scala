package $organization$.persistence.postgres

import cats.effect.{Blocker, Resource}
import cats.effect.concurrent.Ref
import cats.mtl.implicits._
import cats.syntax.flatMap._
import $organization$.test.BaseSpec
import $organization$.util.execution.Retry
import $organization$.util.error.RaisedError
import doobie.hikari.HikariTransactor
import eu.timepit.refined.auto._

import scala.concurrent.duration._

class TransactorResourceSpec extends BaseSpec {

  "TransactorResource" should {

    "return an error" when {

      "connection in unreachable" in EffectAssertion() {
        def mkResource(counter: Ref[Eff, Int]): TransactorResource[Eff] = new TransactorResource[Eff] {
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

        def fa(counter: Ref[Eff, Int]): Resource[Eff, HikariTransactor[Eff]] =
          for {
            blocker    <- Blocker[Eff]
            transactor <- mkResource(counter).createAndVerify(config, blocker)
          } yield transactor

        for {
          counter <- Ref.of[Eff, Int](0)
          result  <- fa(counter).use(_ => Eff.unit).attemptHandle[RaisedError]
          retries <- counter.get
        } yield {
          result.leftValue.error.select[PostgresError].value
          retries shouldBe 6
        }
      }

    }

  }

}
