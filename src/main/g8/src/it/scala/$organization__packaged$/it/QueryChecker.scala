package $organization$.it

import cats.effect.Effect
import $organization$.persistence.postgres.PostgresConfig
import $organization$.util.execution.Eff
import $organization$.util.syntax.config._
import com.typesafe.config.ConfigFactory
import doobie.scalatest.Checker
import doobie.syntax.connectionio._
import doobie.util.testing.{analyze, formatReport, Analyzable}
import doobie.util.transactor.Transactor
import eu.timepit.refined.auto._

trait QueryChecker extends Checker[Eff] {
  self: ITSpec =>

  override implicit val M: Effect[Eff] = self.Eff

  override lazy val transactor: doobie.Transactor[Eff] = {
    val config = ConfigFactory.load().load[PostgresConfig]("application.persistence.postgres").value
    Transactor.fromDriverManager[Eff](config.driver, config.uri, config.user, config.password)
  }

  def checkQuery[A: Analyzable](a: A): Eff[Unit] = {
    val args = Analyzable.unpack(a)

    analyze(args).transact(transactor).flatMap {
      case report if report.succeeded => Eff.unit
      case report                     => Eff.delay(fail(formatReport(args, report, colors).padLeft("  ").toString))
    }
  }

}
