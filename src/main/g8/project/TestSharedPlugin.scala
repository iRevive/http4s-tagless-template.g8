import org.scalafmt.sbt.ScalafmtPlugin
import sbt.Keys._
import sbt._
import scalafix.sbt.ScalafixPlugin.autoImport.scalafixConfigSettings

object TestSharedPlugin extends AutoPlugin {

  override def trigger = noTrigger

  object autoImport {
    lazy val TestShared = Configuration.of("TestShared", "test-shared").extend(Runtime)
  }

  import autoImport._

  override def projectConfigurations: Seq[Configuration] =
    Seq(TestShared, IntegrationTest)

  override def projectSettings: Seq[Setting[_]] = {
    val testSharedSettings = inConfig(TestShared) {
      Defaults.configSettings ++ ScalafmtPlugin.scalafmtConfigSettings ++ scalafixConfigSettings(TestShared)
    }

    val itSettings = inConfig(IntegrationTest) {
      Defaults.testSettings ++ ScalafmtPlugin.scalafmtConfigSettings ++ scalafixConfigSettings(IntegrationTest)
    }

    val extra = Seq(
      Test / dependencyClasspath := (Test / dependencyClasspath).value ++ (TestShared / exportedProducts).value,
      IntegrationTest / dependencyClasspath := (IntegrationTest / dependencyClasspath).value ++ (TestShared / exportedProducts).value
    )

    testSharedSettings ++ itSettings ++ extra
  }

}
