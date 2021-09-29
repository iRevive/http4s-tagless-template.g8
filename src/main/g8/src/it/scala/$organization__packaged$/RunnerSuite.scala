package $organization$

import cats.data.{EitherT, Kleisli}
import cats.effect.{Async, ExitCode, IO}
import cats.syntax.either.*
import $organization$.it.{AppSuite, EffUnsafeRun}
import $organization$.persistence.postgres.PostgresError
import $organization$.util.error.{AppError, RaisedError}
import $organization$.util.execution.Eff
import io.odin.meta.Position
import weaver.SimpleMutableIOSuite

object RunnerSuite extends SimpleMutableIOSuite {

  test("execute a job via main method") {
    val runner = new Runner.Simple {
      override def name: String = "test"

      def job(resource: Application[Eff]): Eff[ExitCode] =
        Async[Eff].pure(ExitCode.Success)
    }

    for {
      r <- IO(runner.main(Array.empty)).attempt
    } yield expect(r.isRight)
  }

  test("rethrow an exception") {
    val exception = new RuntimeException("Unhandled error")

    val runner = new Runner.Simple {
      override def name: String = "test"

      def job(resource: Application[Eff]): Eff[ExitCode] =
        Async[Eff].raiseError(exception)
    }

    for {
      r <- runner.run(Nil).attempt
    } yield expect(r == Left(exception))
  }

  test("return checked error") {
    val error = RaisedError(
      PostgresError.ConnectionAttemptTimeout("error"),
      Position.derivePosition,
      "errorId"
    )

    val runner = new Runner.Simple {
      override def name: String = "test"

      def job(resource: Application[Eff]): Eff[ExitCode] =
        Kleisli.liftF(EitherT.leftT(error))
    }

    for {
      r <- runner.run(Nil).attempt
    } yield expect(r.leftMap(_.getMessage) == Left(error.toException.getMessage))
  }

  test("respect cancellation") {
    val runner = new Runner.Simple {
      override def name: String = "test"

      def job(resource: Application[Eff]): Eff[ExitCode] =
        Async[Eff].never[ExitCode]
    }

    for {
      r <- runner.run(Nil).start.flatMap(f => f.cancel).attempt
    } yield expect(r.isRight)
  }

}
