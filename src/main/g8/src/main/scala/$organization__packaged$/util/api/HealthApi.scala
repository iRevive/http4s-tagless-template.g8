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
//import sup.modules.circe._
import sup.modules.doobie._
import sup.modules.http4s._
import sup.{mods, Health, HealthCheck, HealthReporter}

class HealthApi[F[_]: Sync](transactor: HikariTransactor[F]) extends Http4sDsl[F] {

  import HealthCheckEncoder._

  lazy val routes: HttpRoutes[F] = healthCheckRoutes(reporter, "health")

  private def reporter: HealthReporter[F, NonEmptyList, Tagged[String, *]] =
    HealthReporter.fromChecks(doobieCheck, apiCheck)

  private def apiCheck: HealthCheck[F, Tagged[String, *]] =
    HealthCheck.const[F, Id](Health.Healthy).through(mods.tagWith("api"))

  private def doobieCheck: HealthCheck[F, Tagged[String, *]] =
    connectionCheck(transactor)(Some(5))
      .through(mods.recoverToSick)
      .through(mods.tagWith("postgres"))

}

// todo remove after sup 0.7.0
private object HealthCheckEncoder {

  import sup.HealthResult
  import sup.data.Report
  import io.circe.generic.semiauto.deriveEncoder
  import io.circe.Encoder

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  implicit val healthCirceEncoder: Encoder[Health] = Encoder[String].contramap(_.toString)

  implicit def taggedCirceEncoder[Tag: Encoder, H: Encoder]: Encoder[Tagged[Tag, H]] = deriveEncoder

  implicit def reportCirceEncoder[G[_], H[_], A: Encoder](implicit H: Encoder[G[H[A]]]): Encoder[Report[G, H, A]] = {
    val _ = H
    deriveEncoder
  }

  implicit def healthResultCirceEncoder[H[_]](implicit E: Encoder[H[Health]]): Encoder[HealthResult[H]] =
    E.contramap(_.value)

}
