package $organization$.util.error

import $organization$.test.BaseSpec
import $organization$.persistence.postgres.PostgresError
import $organization$.util.logging.Loggable

class RaisedErrorSpec extends BaseSpec {

  "RaisedError" should {

    "use correct loggable instance" in {
      val message = randomString()
      val error   = RaisedError.postgres(PostgresError.ConnectionAttemptTimeout(message))

      val expectedMessage =
        "RaisedError(" +
          s"error = ConnectionAttemptTimeout(message = \$message), " +
          "pos = $organization$.util.error.RaisedErrorSpec#error:13)"

      Loggable[RaisedError].show(error) shouldBe expectedMessage
    }

    "create a runtime exception" in {
      val message = randomString()
      val error   = RaisedError.postgres(PostgresError.ConnectionAttemptTimeout(message))

      val asException = error.toException

      val expectedMessage =
        "RaisedError(" +
          s"error = ConnectionAttemptTimeout(message = \$message), " +
          "pos = $organization$.util.error.RaisedErrorSpec#error:25)"

      asException shouldBe a[RuntimeException]
      asException.getMessage shouldBe expectedMessage
    }

  }

}
