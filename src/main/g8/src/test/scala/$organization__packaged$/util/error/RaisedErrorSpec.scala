package $organization$.util.error

import $organization$.persistence.postgres.PostgresError
import $organization$.test.BaseSpec
import $organization$.util.logging.Loggable
import shapeless.syntax.inject._

class RaisedErrorSpec extends BaseSpec {

  "RaisedError" should {

    "use correct loggable instance" in forAll { (message: String, errorId: String) =>
      EffectAssertion() {
        implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const(errorId)

        val expectedMessage =
          "RaisedError(" +
            s"error = ConnectionAttemptTimeout(message = \$message), " +
            s"pos = com.example.util.error.RaisedErrorSpec#:22, errorId = \$errorId)"

        for {
          error <- RaisedError.withErrorId[Eff](PostgresError.connectionAttemptTimeout(message).inject[AppError])
        } yield Loggable[RaisedError].show(error) shouldBe expectedMessage
      }
    }

    "create a runtime exception" in forAll { (message: String, errorId: String) =>
      EffectAssertion() {
        implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const(errorId)

        val expectedMessage =
          "RaisedError(" +
            s"error = ConnectionAttemptTimeout(message = \$message), " +
            s"pos = com.example.util.error.RaisedErrorSpec#:37, errorId = \$errorId)"

        for {
          error <- RaisedError.withErrorId[Eff](PostgresError.connectionAttemptTimeout(message).inject[AppError])
        } yield {
          val asException = error.toException

          asException shouldBe a[RuntimeException]
          asException.getMessage shouldBe expectedMessage
        }
      }
    }

  }

}
