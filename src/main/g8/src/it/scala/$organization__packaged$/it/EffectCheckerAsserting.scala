package $organization$.test

import cats.MonadError
import org.scalactic.source
import org.scalatest.exceptions.{GeneratorDrivenPropertyCheckFailedException, StackDepthException}
import org.scalatestplus.scalacheck.CheckerAsserting

class EffectCheckerAsserting[F[_], A](implicit M: MonadError[F, Throwable]) extends CheckerAsserting.CheckerAssertingImpl[A] {

  type Result = F[Unit]

  override def indicateSuccess(message: => String): Result = M.unit

  override def indicateFailure(
      messageFun: StackDepthException => String,
      undecoratedMessage: => String,
      scalaCheckArgs: List[Any],
      scalaCheckLabels: List[String],
      optionalCause: Option[Throwable],
      pos: source.Position
  ): Result = {
    val error = new GeneratorDrivenPropertyCheckFailedException(
      messageFun,
      optionalCause,
      pos,
      None,
      undecoratedMessage,
      scalaCheckArgs,
      None,
      scalaCheckLabels
    )

    M.raiseError(error)
  }

}
