package $organization$.util.error

import $organization$.persistence.postgres.PostgresError
import $organization$.test.SimpleEffSuite
import io.odin.meta.Render

object RaisedErrorSuite extends SimpleEffSuite {

  test("use correct render instance") {
    forall { (message: String, errorId: String) =>
      implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const(errorId)

      val expectedMessage =
        "RaisedError(" +
          s"error = ConnectionAttemptTimeout(message = \$message), " +
          s"pos = $organization$.util.error.RaisedErrorSuite:19, errorId = \$errorId)"

      for {
        error <- RaisedError.withErrorId[Eff](PostgresError.ConnectionAttemptTimeout(message))
      } yield expect(Render[RaisedError].render(error) == expectedMessage)
    }
  }

  test("create a runtime exception") {
    forall { (message: String, errorId: String) =>
      implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const(errorId)

      val expectedMessage =
        "RaisedError(" +
          s"error = ConnectionAttemptTimeout(message = \$message), " +
          s"pos = $organization$.util.error.RaisedErrorSuite:34, errorId = \$errorId)"

      for {
        error       <- RaisedError.withErrorId[Eff](PostgresError.ConnectionAttemptTimeout(message))
        asException <- Eff.pure(error.toException)
      } yield expect.all(asException.isInstanceOf[RuntimeException], asException.getMessage == expectedMessage)
    }
  }

}
