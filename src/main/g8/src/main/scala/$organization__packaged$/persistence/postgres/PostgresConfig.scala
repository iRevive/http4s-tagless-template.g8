package $organization$.persistence.postgres

import $organization$.util.execution.Retry
import $organization$.util.instances.render.*
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.refined.*
import io.odin.extras.derivation.*
import io.odin.meta.Render
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

import scala.concurrent.duration.FiniteDuration

final case class PostgresConfig(
    driver: String,
    uri: String,
    @secret user: String,
    @secret password: String,
    connectionAttemptTimeout: FiniteDuration,
    runMigration: Boolean,
    retryPolicy: Retry.Policy
) derives ConfigReader

object PostgresConfig {
  given Render[PostgresConfig] = {
    import io.odin.extras.derivation.render.derived
    Render.derived
  }
}
