package $organization$.util

import $organization$.util.logging.Loggable
import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.Decoder
import io.circe.config.syntax.durationDecoder
import io.circe.refined._

import scala.concurrent.duration.FiniteDuration

@scalaz.deriving(Decoder, Loggable)
final case class RetryPolicy(retries: NonNegInt, delay: FiniteDuration, timeout: FiniteDuration)
