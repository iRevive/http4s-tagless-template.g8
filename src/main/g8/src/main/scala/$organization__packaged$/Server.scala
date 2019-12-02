package $organization$

import java.time.Instant

import cats.data.Kleisli
import cats.effect._
import cats.mtl.implicits._
import cats.syntax.flatMap._
import $organization$.ApplicationResource.Application
import $organization$.util.api.ApiConfig
import $organization$.util.execution.Eff
import $organization$.util.logging.TracedLogger
import $organization$.util.syntax.logging._
import eu.timepit.refined.auto.autoUnwrap
import org.http4s.server.blaze.BlazeServerBuilder

// \$COVERAGE-OFF\$
object Server extends Runner.Default {

  override lazy val name: String = log"Server-\${Instant.now}"

  override def job: Kleisli[Eff, Application[Eff], ExitCode] = Kleisli { app =>
    val ApiConfig(host, port, _) = app.apiModule.config

    val server = BlazeServerBuilder[Eff]
      .bindHttp(port, host)
      .withHttpApp(app.apiModule.httpApp)
      .resource

    for {
      _ <- logger.info(log"Application trying to bind to host [\$host:\$port]")
      _ <- server.use(_ => logger.info(log"Application bound to [\$host:\$port]") >> Eff.never[Unit])
    } yield ExitCode.Success
  }

  private val logger: TracedLogger[Eff] = TracedLogger.create[Eff](getClass)

}
// \$COVERAGE-ON\$
