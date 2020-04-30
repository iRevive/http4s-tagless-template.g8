package $organization$

import java.time.Instant

import cats.data.Kleisli
import cats.effect._
import cats.mtl.instances.local._
import $organization$.ApplicationResource.{ApiModule, Application}
import $organization$.util.api.ApiConfig
import $organization$.util.execution.Eff
import $organization$.util.instances.render._
import $organization$.util.logging.{Loggers, TraceProvider}
import eu.timepit.refined.auto._
import io.odin.{Level, Logger}
import io.odin.syntax._
import monix.execution.Scheduler
import org.http4s.server.blaze.BlazeServerBuilder

// \$COVERAGE-OFF\$
object Server extends Runner.Default {

  override lazy val name: String = render"Server-\${Instant.now}"

  override def job: Kleisli[Eff, Application[Eff], ExitCode] = new Server[Eff].serve

}

class Server[F[_]: ConcurrentEffect: Timer: TraceProvider](scheduler: Scheduler) {

  def serve: Kleisli[F, Application[F], ExitCode] = Kleisli { app =>
    bindHttpServer(app.apiModule).use(_ => ConcurrentEffect[F].never)
  }

  private def bindHttpServer(apiModule: ApiModule[F]): Resource[F, Unit] = {
    val ApiConfig(host, port, _) = apiModule.config

    val server = BlazeServerBuilder[F](scheduler)
      .bindHttp(port, host)
      .withHttpApp(apiModule.httpApp)
      .resource

    for {
      _ <- Resource.liftF(logger.info(render"Application trying to bind to host [\$host:\$port]"))
      _ <- server
      _ <- Resource.liftF(logger.info(render"Application bound to [\$host:\$port]"))
    } yield ()
  }

  private val logger: Logger[F] = Loggers.consoleContextLogger(Level.Info)

}
// \$COVERAGE-ON\$
