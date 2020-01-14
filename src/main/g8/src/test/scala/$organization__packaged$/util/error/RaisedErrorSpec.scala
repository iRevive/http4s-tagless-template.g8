package $organization$.util.error

import $organization$.persistence.postgres.PostgresError
import $organization$.test.BaseSpec
import shapeless.syntax.inject._
import io.odin.meta.Render

class RaisedErrorSpec extends BaseSpec {

  "RaisedError" should {

    "use correct render instance" in EffectAssertion() {
      forAll { (message: String, errorId: String) =>
        implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const(errorId)

        val expectedMessage =
          "RaisedError(" +
            s"error = ConnectionAttemptTimeout(message = \$message), " +
            s"pos = $organization$.util.error.RaisedErrorSpec:22, errorId = \$errorId)"

        for {
          error <- RaisedError.withErrorId[Eff](PostgresError.connectionAttemptTimeout(message).inject[AppError])
        } yield Render[RaisedError].render(error) shouldBe expectedMessage
      }
    }

    "create a runtime exception" in EffectAssertion() {
      forAll { (message: String, errorId: String) =>
        implicit val errorIdGen: ErrorIdGen[Eff] = ErrorIdGen.const(errorId)

        val expectedMessage =
          "RaisedError(" +
            s"error = ConnectionAttemptTimeout(message = \$message), " +
            s"pos = $organization$.util.error.RaisedErrorSpec:37, errorId = \$errorId)"

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
