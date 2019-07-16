package $organization$

import java.time.Instant

import cats.data.Kleisli
import cats.effect._
import cats.mtl.implicits._
import cats.syntax.flatMap._
import $organization$.ApplicationLoader.Application
import $organization$.util.api.ApiConfig
import $organization$.util.logging.TracedLogger
import $organization$.util.syntax.logging._
import eu.timepit.refined.auto.autoUnwrap
import org.http4s.server.blaze.BlazeServerBuilder

// \$COVERAGE-OFF\$
object Server extends Runner.Default {

  override lazy val name: String = s"Server-\${Instant.now}"

  override def job: Kleisli[F, Application[F], ExitCode] = Kleisli { app =>
    val ApiConfig(host, port, _) = app.apiModule.config

    val server = BlazeServerBuilder[F]
      .bindHttp(port, host)
      .withHttpApp(app.apiModule.httpApp)
      .resource

    for {
      _ <- logger.info(log"Application trying to bind to host [\$host:\$port]")
      _ <- server.use(_ => logger.info(log"Application bound to [\$host:\$port]") >> F.never[Unit])
    } yield ExitCode.Success
  }

  private val logger: TracedLogger[F] = TracedLogger.create[F](getClass)

}
// \$COVERAGE-ON\$
