package $organization$.util.error

import $organization$.test.{BaseSpec, GenRandom}
import $organization$.persistence.postgres.PostgresError
import $organization$.util.logging.Loggable
import shapeless.syntax.inject._

class RaisedErrorSpec extends BaseSpec {

  "RaisedError" should {

    "use correct loggable instance" in {
      val message = GenRandom[String].gen
      val errorId = GenRandom[String].gen

      val error = RaisedError
        .withErrorId(PostgresError.connectionAttemptTimeout(message).inject[AppError])
        .copy(errorId = errorId)

      val expectedMessage =
        "RaisedError(" +
          s"error = ConnectionAttemptTimeout(message = \$message), " +
          s"pos = $organization$.util.error.RaisedErrorSpec#error:17, errorId = \$errorId)"

      Loggable[RaisedError].show(error) shouldBe expectedMessage
    }

    "create a runtime exception" in {
      val message = GenRandom[String].gen
      val errorId = GenRandom[String].gen

      val error = RaisedError
        .withErrorId(PostgresError.connectionAttemptTimeout(message).inject[AppError])
        .copy(errorId = errorId)

      val asException = error.toException

      val expectedMessage =
        "RaisedError(" +
          s"error = ConnectionAttemptTimeout(message = \$message), " +
          s"pos = $organization$.util.error.RaisedErrorSpec#error:33, errorId = \$errorId)"

      asException shouldBe a[RuntimeException]
      asException.getMessage shouldBe expectedMessage
    }

  }

}
