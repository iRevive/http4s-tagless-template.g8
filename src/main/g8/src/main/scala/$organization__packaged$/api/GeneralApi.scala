package $organization$.api

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

class GeneralApi[F[_]: Sync] extends Http4sDsl[F] {

  lazy val routes: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root / "health" =>
      Ok("I'm alive")
  }

}
