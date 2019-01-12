package $organization$.util.config

import $organization$.util.logging.Loggable
import io.circe.Decoder

/**
  * See https://github.com/circe/circe-config/issues/12
  */
final case class ConfigBoolean(value: Boolean)

object ConfigBoolean {

  implicit def toBoolean(b: ConfigBoolean): Boolean = b.value

  implicit def fromBoolean(b: Boolean): ConfigBoolean = ConfigBoolean(b)

  implicit val configBooleanDecoder: Decoder[ConfigBoolean] = {
    val truthful = Set("true", "yes", "on")

    val booleanDecoder = Decoder.decodeBoolean.map(ConfigBoolean.apply)
    val stringDecoder  = Decoder.decodeString.map(s => ConfigBoolean(truthful.contains(s)))

    booleanDecoder.or(stringDecoder)
  }

  implicit val loggableInstance: Loggable[ConfigBoolean] = Loggable.instance(_.value.toString)

}
