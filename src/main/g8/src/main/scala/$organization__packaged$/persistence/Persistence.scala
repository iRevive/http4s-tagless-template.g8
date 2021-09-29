package $organization$.persistence

import cats.effect.kernel.{Async, Resource}
import $organization$.persistence.postgres.TransactorResource
import $organization$.util.ConfigSource
import $organization$.util.error.ErrorChannel
import $organization$.util.trace.TraceProvider
import doobie.hikari.HikariTransactor
import io.odin.Logger

final case class Persistence[F[_]](transactor: HikariTransactor[F])

object Persistence {

  def create[F[_]: Async: ErrorChannel: TraceProvider: Logger](config: ConfigSource[F]): Resource[F, Persistence[F]] =
    for {
      transactor <- TransactorResource.create[F](config)
    } yield Persistence(transactor)

}
