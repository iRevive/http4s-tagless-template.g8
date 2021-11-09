package $organization$

import java.time.Instant

import cats.data.Kleisli
import cats.effect.*
import $organization$.Application
import $organization$.util.api.ApiConfig
import $organization$.util.instances.render.*
import $organization$.util.logging.Loggers
import $organization$.util.trace.TraceProvider
import eu.timepit.refined.auto.*
import io.odin.syntax.*
import io.odin.{Level, Logger}
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext

// \$COVERAGE-OFF\$
object Server extends Runner.Simple {

  override lazy val name: String = render"Server-\${Instant.now}"

  override def job(app: Application[Eff]): Eff[ExitCode] =
    bindHttpServer(app).use(_ => effect.never)

  private def bindHttpServer(app: Application[Eff]): Resource[Eff, Unit] = {
    val ApiConfig(host, port, _) = app.api.config

    val server = BlazeServerBuilder[Eff](runtime.compute)
      .bindHttp(port, host)
      .withHttpApp(app.api.httpApp)
      .resource

    for {
      _ <- Resource.eval(app.logger.info(render"Application trying to bind to host [\$host:\$port]"))
      _ <- server
      _ <- Resource.eval(app.logger.info(render"Application bound to [\$host:\$port]"))
    } yield ()
  }

}
// \$COVERAGE-ON\$
