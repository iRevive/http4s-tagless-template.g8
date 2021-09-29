/*package $organization$.util.logging

import java.net.InetSocketAddress

import cats.effect.{Concurrent, ContextShift, Resource, Sync, Timer}
import cats.syntax.functor.*
import cats.{Monad, MonadError}
import fs2.io.tcp.{Socket, SocketGroup}
import io.odin.LoggerMessage
import io.odin.formatter.Formatter
import javax.net.ssl.SSLContext
import spinoco.fs2.crypto.io.tcp.TLSSocket

class LogentriesLogger[F[_]: Monad: Timer](
    formatter: Formatter,
    client: LogentriesClient[F]
)(implicit F: MonadError[F, Throwable])
    extends _root_.io.odin.loggers.DefaultLogger[F] {

  override def log(msg: LoggerMessage): F[Unit] = {
    client.send(formatter.format(msg))
  }

}

class LogentriesClient[F[_]: Sync](token: String, socket: Socket[F]) {

  def send(message: String): F[Unit] = {
    println(s"\${java.time.Instant.now} Send")
    fs2
      .Stream(s"\$token\$message\\n")
      .through(fs2.text.utf8Encode)
      .through(socket.writes())
      .compile
      .drain
  }

}

object LogentriesClient {

  val DATA_ENDPOINT_TEMPLATE = "%s.data.logs.insight.rapid7.com"

  val token = "43721a23-420c-4982-b991-742795fa5bc7"

  def create[F[_]: Concurrent: ContextShift](socketGroup: SocketGroup, token: String): Resource[F, LogentriesClient[F]] =
    socketGroup.client(new InetSocketAddress("data.logentries.com", 443), keepAlive = true).evalMap { s =>
      TLSSocket.instance(s, engine, scala.concurrent.ExecutionContext.global).map { socket =>
        new LogentriesClient[F](token, socket)
      }
    }

  //TLSContext

  val sslCtx = SSLContext.getInstance("TLS")
  sslCtx.init(null, null, null)

  val ctx = SSLContext.getInstance("TLS")
  ctx.init(null, null, null)

  val engine = sslCtx.createSSLEngine()
  engine.setUseClientMode(true)

}*/
