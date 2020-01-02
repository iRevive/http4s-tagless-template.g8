package $organization$.test

import cats.effect.ConcurrentEffect
import cats.effect.testing.scalatest.scalacheck.EffectCheckerAsserting
import $organization$.util.error.RaisedError
import $organization$.util.execution.EffConcurrentEffect
import org.scalatestplus.scalacheck.CheckerAsserting

trait BaseSpec extends EffectSpec[RaisedError] {

  protected override implicit lazy val Eff: ConcurrentEffect[Eff] = new EffConcurrentEffect

  protected implicit final def effCheckingAsserting[A]: CheckerAsserting[Eff[A]] { type Result = Eff[Unit] } =
    new EffectCheckerAsserting

}
