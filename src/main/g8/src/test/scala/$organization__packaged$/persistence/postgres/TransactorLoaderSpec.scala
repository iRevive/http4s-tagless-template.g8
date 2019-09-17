package $organization$.persistence.postgres

import cats.effect.concurrent.Ref
import cats.mtl.implicits._
import cats.syntax.flatMap._
import $organization$.test.BaseSpec
import $organization$.util.execution.Retry
import $organization$.util.error.ErrorHandle
import doobie.hikari.HikariTransactor
import eu.timepit.refined.auto._

import scala.concurrent.duration._

class TransactorLoaderSpec extends BaseSpec {

  "TransactorLoader" should {

    "return an error" when {

      "connection in unreachable" in EffectAssertion() {
        def mkLoader(counter: Ref[Eff, Int]): TransactorLoader[Eff] = new TransactorLoader[Eff] {
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
          retryPolicy = Retry.Policy(5, 30.millis, 5.second)
        )

        for {
          counter <- Ref.of[Eff, Int](0)
          loader = mkLoader(counter)
          result  <- ErrorHandle[Eff].attempt(loader.createAndVerify(config).use(_ => Eff.unit))
          retries <- counter.get
        } yield {
          result.leftValue.error.select[PostgresError].value
          retries shouldBe 5
        }
      }

    }

  }

}
