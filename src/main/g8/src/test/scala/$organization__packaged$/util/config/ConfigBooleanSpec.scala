package $organization$.util.config

import $organization$.util.logging.Loggable
import com.typesafe.config.ConfigFactory
import io.circe.config.syntax._
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfigBooleanSpec extends AnyWordSpec with Matchers {

  import ConfigBooleanSpec._

  "ConfigBoolean" should {

    "be parsed and decoded from string" in {
      val config = ConfigFactory.parseString(
        """
        host = localhost
        port = 8080
        enabled = on
        enabled2 = true
        """
      )
      val Right(ServerConfig(_, _, enabled, enabled2)) = config.as[ServerConfig]
      assert(enabled.value)
      assert(enabled)

      assert(enabled2.value)
      assert(enabled2)
    }

    "behave like boolean" in {
      (true: ConfigBoolean) shouldBe ConfigBoolean(true)
      (false: ConfigBoolean) shouldBe ConfigBoolean(false)
    }

    "return flat value " in {
      Loggable[ConfigBoolean].show(true) shouldBe "true"
      Loggable[ConfigBoolean].show(false) shouldBe "false"
    }

  }

}

object ConfigBooleanSpec {

  final case class ServerConfig(host: String, port: Int, enabled: ConfigBoolean, enabled2: ConfigBoolean)

}
