import org.scalafmt.sbt.ScalafmtPlugin
import sbt.Keys._
import sbt._

object IntegrationEnv extends AutoPlugin {

  override def trigger = noTrigger

  object autoImport {

    sealed trait EnvMode {

      def fold[A](ci: => A, dev: => A): A =
        this match {
          case EnvMode.CI  => ci
          case EnvMode.Dev => dev
        }

    }

    object EnvMode {
      final case object CI  extends EnvMode
      final case object Dev extends EnvMode
    }

    lazy val envMode    = settingKey[EnvMode]("Env mode. CI - cleanup env after execution. Dev - keep env alive.")
    lazy val startItEnv = taskKey[Unit]("Start integration environment")
    lazy val stopItEnv  = taskKey[Unit]("Stop integration environment")
  }

  import autoImport._

  override lazy val projectConfigurations: Seq[Configuration] = Seq(
    IntegrationTest
  )

  override lazy val projectSettings: Seq[Setting[_]] = {
    lazy val env = DockerEnvironment.createEnv(sys.env.get("DOCKER_NETWORK"))

    Seq(
      envMode    := (if (sys.env.get("ENV_MODE").contains("CI")) EnvMode.CI else EnvMode.Dev),
      stopItEnv  := env.destroy(sourceDirectory.value),
      startItEnv := env.start(sourceDirectory.value),
      IntegrationTest / testOptions ++= Def.task {
        val mode      = envMode.value
        val log       = streams.value.log
        val sourceDir = sourceDirectory.value

        val setup = Tests.Setup { () =>
          log.info(s"Re-creating integration environment. Mode [$mode]")
          mode.fold(
            {
              env.destroy(sourceDir)
              env.start(sourceDir)
            },
            env.start(sourceDir)
          )
        }

        val cleanup = Tests.Cleanup { () =>
          log.info(s"Destroying integration environment. Mode [$mode]")
          mode.fold(env.destroy(sourceDir), ())
        }

        Seq(setup, cleanup)
      }.value,
      IntegrationTest / fork              := true,
      IntegrationTest / parallelExecution := false,
      IntegrationTest / javaOptions       := env.javaOpts
    ) ++ Defaults.itSettings ++ inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings)
  }

}
