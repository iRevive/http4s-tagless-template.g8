package $organization$.util.api

import cats.Id
import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.hikari.HikariTransactor
import eu.timepit.refined.auto._
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

  // TODO use healthCheckRoutes(reporter, "health") after sup 0.7.0 release
  lazy val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" =>
      healthCheckResponse(reporter)
  }

  private def reporter: HealthReporter[F, NonEmptyList, Tagged[String, *]] =
    HealthReporter.fromChecks(doobieCheck, apiCheck)

  private def apiCheck: HealthCheck[F, Tagged[String, *]] =
    HealthCheck.const[F, Id](Health.Healthy).through(mods.tagWith("api"))

  private def doobieCheck: HealthCheck[F, Tagged[String, *]] =
    connectionCheck(transactor)(Some(5.seconds))
      .through(mods.recoverToSick)
      .through(mods.tagWith("postgres"))

}
