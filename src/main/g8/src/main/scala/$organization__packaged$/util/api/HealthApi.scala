package $organization$.util.api

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.{Id, catsInstancesForId}
import doobie.hikari.HikariTransactor
import eu.timepit.refined.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import sup.data.{HealthReporter, Tagged}
import sup.modules.circe.*
import sup.modules.doobie.*
import sup.modules.http4s.*
import sup.{Health, HealthCheck, HealthReporter, mods}

import scala.concurrent.duration.*

class HealthApi[F[_]: Sync](transactor: HikariTransactor[F]) extends Http4sDsl[F] {

  lazy val routes: HttpRoutes[F] = healthCheckRoutes(reporter, "health")

  private def reporter: HealthReporter[F, NonEmptyList, Tagged[String, *]] =
    HealthReporter.fromChecks(postgresCheck, apiCheck)

  private def apiCheck: HealthCheck[F, Tagged[String, *]] =
    HealthCheck.const[F, Id](Health.Healthy).through(mods.tagWith("api"))

  private def postgresCheck: HealthCheck[F, Tagged[String, *]] =
    connectionCheck(transactor)(Some(5.seconds))
      .through(mods.recoverToSick)
      .through(mods.tagWith("postgres"))

}
