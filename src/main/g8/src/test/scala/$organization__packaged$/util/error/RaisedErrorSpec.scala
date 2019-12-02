package $organization$.util.error

import $organization$.persistence.postgres.PostgresError
import $organization$.test.BaseSpec
import $organization$.util.logging.Loggable
import shapeless.syntax.inject._

class RaisedErrorSpec extends BaseSpec {

  "RaisedError" should {

    "use correct loggable instance" in forAll { (message: String, errorId: String) =>
      implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const(errorId)

      val expectedMessage =
        "RaisedError(" +
          s"error = ConnectionAttemptTimeout(message = \$message), " +
          s"pos = $organization$.util.error.RaisedErrorSpec#:21, errorId = \$errorId)"

      for {
        error <- RaisedError.withErrorId[Eff](PostgresError.connectionAttemptTimeout(message).inject[AppError])
      } yield Loggable[RaisedError].show(error) shouldBe expectedMessage
    }

    "create a runtime exception" in forAll { (message: String, errorId: String) =>
      implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const(errorId)

      val expectedMessage =
        "RaisedError(" +
          s"error = ConnectionAttemptTimeout(message = \$message), " +
          s"pos = $organization$.util.error.RaisedErrorSpec#:34, errorId = \$errorId)"

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
