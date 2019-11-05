package $organization$.util.api

import cats.Id
import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.hikari.HikariTransactor
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import sup.data.Tagged
import sup.modules.circe._
import sup.modules.doobie._
import sup.modules.http4s._
import sup.{mods, Health, HealthCheck, HealthReporter}

import scala.concurrent.duration._

class HealthApi[F[_]: Sync](transactor: HikariTransactor[F]) extends Http4sDsl[F] {

  lazy val routes: HttpRoutes[F] = healthCheckRoutes(reporter, "health")

  private def reporter: HealthReporter[F, NonEmptyList, Tagged[String, *]] =
    HealthReporter.fromChecks(doobieCheck, apiCheck)

  private def apiCheck: HealthCheck[F, Tagged[String, *]] =
    HealthCheck.const[F, Id](Health.Healthy).through(mods.tagWith("api"))

  private def doobieCheck: HealthCheck[F, Tagged[String, *]] =
    connectionCheck(transactor)(timeout = Some(5.seconds))
      .through(mods.recoverToSick)
      .through(mods.tagWith("postgres"))

}
