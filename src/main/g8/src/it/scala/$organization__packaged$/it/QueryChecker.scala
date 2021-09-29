package $organization$.it

import cats.effect.Async
import $organization$.Application
import $organization$.persistence.postgres.PostgresConfig
import $organization$.util.ConfigSource
import $organization$.util.execution.Eff
import com.typesafe.config.ConfigFactory
import doobie.syntax.connectionio.*
import doobie.util.Colors
import doobie.util.testing.{Analyzable, UnsafeRun as DobieUnsafeRun, analyze, formatReport}
import eu.timepit.refined.auto.*
import weaver.{Expectations, SourceLocation}

trait QueryChecker {
  self: AppSuite =>

  def checkQuery[A: Analyzable](a: A)(implicit app: Application[Eff], pos: SourceLocation): Eff[Expectations] = {
    val args = Analyzable.unpack(a)

    analyze(args).transact(app.persistence.transactor).map {
      case report if report.succeeded => success
      case report                     => failure(formatReport(args, report, Colors.Ansi).padLeft("  ").toString)
    }
  }

}
