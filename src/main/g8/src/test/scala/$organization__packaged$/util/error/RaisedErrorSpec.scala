package $organization$.util.error

import $organization$.persistence.postgres.PostgresError
import $organization$.test.BaseSpec
import $organization$.util.logging.Loggable
import shapeless.syntax.inject._

class RaisedErrorSpec extends BaseSpec {

  "RaisedError" should {

    "use correct loggable instance" in EffectAssertion() {
      forAll { (message: String, errorId: String) =>
        val expectedMessage =
          "RaisedError(" +
            s"error = ConnectionAttemptTimeout(message = \$message), " +
            s"pos = com.example.util.error.RaisedErrorSpec#:20, errorId = \$errorId)"

        for {
          error <- RaisedError.withErrorId[Eff](PostgresError.connectionAttemptTimeout(message).inject[AppError])
        } yield Loggable[RaisedError].show(error.copy(errorId = errorId)) shouldBe expectedMessage
      }
    }

    "create a runtime exception" in EffectAssertion() {
      forAll { (message: String, errorId: String) =>
        val expectedMessage =
          "RaisedError(" +
            s"error = ConnectionAttemptTimeout(message = \$message), " +
            s"pos = com.example.util.error.RaisedErrorSpec#:33, errorId = \$errorId)"

        for {
          error <- RaisedError.withErrorId[Eff](PostgresError.connectionAttemptTimeout(message).inject[AppError])
        } yield {
          val asException = error.copy(errorId = errorId).toException

          asException shouldBe a[RuntimeException]
          asException.getMessage shouldBe expectedMessage
        }
      }
    }

  }

}
