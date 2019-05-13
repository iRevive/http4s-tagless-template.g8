package $organization$.api

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import $organization$.util.error.ErrorRaise
import $organization$.util.logging.{TraceProvider, TracedLogger}
import $organization$.util.syntax.config._
import $organization$.util.syntax.logging._
import com.typesafe.config.Config
import org.http4s.HttpApp
import org.http4s.server.Router

class ApiModuleLoader[F[_]: Sync: ErrorRaise: TraceProvider] {

  def load(rootConfig: Config): F[ApiModule[F]] =
    for {
      apiConfig <- rootConfig.loadF[F, ApiConfig]("application.api")
      _         <- logger.info(log"Loading API module with config \$apiConfig")
    } yield ApiModule(routes(), apiConfig)

  private def routes(): HttpApp[F] = {
    import org.http4s.syntax.kleisli._

    val generalApi = new GeneralApi[F]()

    Router("" -> generalApi.routes).orNotFound
  }

  private val logger: TracedLogger[F] = TracedLogger.create[F](getClass)

}
