import org.scalafmt.sbt.ScalafmtPlugin
import sbt.Keys._
import sbt._

object IntegrationEnv extends AutoPlugin {

  override def trigger = noTrigger

  object autoImport {
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
      stopItEnv := {
        env.destroy(sourceDirectory.value)
      },
      startItEnv := {
        env.destroy(sourceDirectory.value)
        env.start(sourceDirectory.value)
      },
      IntegrationTest / testOptions ++= Def.task {
        val log       = streams.value.log
        val sourceDir = sourceDirectory.value

        val setup = Tests.Setup { () =>
          log.info("Re-creating integration environment")
          env.destroy(sourceDir)
          env.start(sourceDir)
        }

        val cleanup = Tests.Cleanup { () =>
          log.info("Destroying integration environment")
          env.destroy(sourceDir)
        }

        Seq(setup, cleanup)
      }.value,
      IntegrationTest / fork              := true,
      IntegrationTest / parallelExecution := false,
      IntegrationTest / javaOptions       := env.javaOpts
    ) ++ Defaults.itSettings ++ inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings)
  }

}
