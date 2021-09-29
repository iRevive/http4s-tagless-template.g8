package $organization$.util.api

import cats.effect.Sync
import cats.syntax.eq.*
import org.http4s.BasicCredentials
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.authentication.BasicAuth

object AuthUtils {

  def basicAuth[F[_]: Sync](config: BasicAuthConfig): AuthMiddleware[F, Unit] =
    BasicAuth(realm = config.realm, performBasicAuth(_, config))

  private def performBasicAuth[F[_]: Sync](credentials: BasicCredentials, config: BasicAuthConfig): F[Option[Unit]] =
    Sync[F].delay {
      val BasicCredentials(username, password, _) = credentials

      Option.when(username === config.user && password === config.password)(())
    }

}
