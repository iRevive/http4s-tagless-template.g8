package $organization$.api

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import $organization$.util.error.ErrorRaise
import $organization$.util.logging.Loggable.InterpolatorOps._
import $organization$.util.logging.{TraceProvider, TracedLogger}
import $organization$.util.syntax.config._
import com.typesafe.config.Config
import org.http4s.HttpApp
import org.http4s.server.Router

class ApiModuleLoader[F[_]: Sync: ErrorRaise: TraceProvider, G[_]: Sync](rootConfig: Config) {

  def loadApiModule(): F[ApiModule[G]] = {
    for {
      apiConfig <- ErrorRaise.fromEither[F](rootConfig.load[ApiConfig]("application.api"))
      _         <- logger.info(log"Loading API module with config \$apiConfig")
    } yield ApiModule(routes(), apiConfig)
  }

  private def routes(): HttpApp[G] = {
    import org.http4s.syntax.kleisli._

    val generalApi = new GeneralApi[G]()

    Router("" -> generalApi.routes).orNotFound
  }

  private val logger = TracedLogger.create[F](getClass)

}
