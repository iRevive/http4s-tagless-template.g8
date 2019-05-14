import sbt.Keys.javaOptions
import sbtrelease.ReleaseStateTransformations._

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, AshScriptPlugin)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings))
  .settings(itEnvironment)
  .settings(commonSettings)
  .settings(scalazDerivingSettings)
  .settings(wartRemoverSettings)
  .settings(testSettings)
  .settings(buildSettings)
  .settings(dockerSettings)
  .settings(releaseSettings)
  .settings(commandSettings)
  .settings(
    name := Settings.name,
    libraryDependencies ++= Dependencies.root
  )

lazy val commonSettings = Seq(
  organization := Settings.organization,
  scalaVersion := Versions.scala,
  scalacOptions -= "-Xfatal-warnings",
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
)

lazy val scalazDerivingSettings = Seq(
  // WORKAROUND for scalaz.deriving: https://github.com/sbt/sbt/issues/1965
  managedClasspath in Compile := {
    val res = (resourceDirectory in Compile).value
    val old = (managedClasspath in Compile).value
    Attributed.blank(res) +: old
  },
  addCompilerPlugin("org.scalaz" %% "deriving-plugin" % Versions.scalazDeriving)
)

lazy val testSettings = Seq(
  fork in Test := true,
  parallelExecution in Test := true,
  javaOptions in Test := Seq(
    "-Dorg.mongodb.async.type=netty"
  )
)

lazy val itEnvironment = {
  import scala.sys.process._
  val startMongo = TaskKey[Unit]("start-mongo", "Start a local MongoDB instance")

  Seq(
    (startMongo in IntegrationTest) := {
      "docker rm -f $name_normalized$-it-mongo".!
      "docker run -d -p 53123:27017 --name $name_normalized$-it-mongo mongo".!
    },

    (test in IntegrationTest) := {
      (test in IntegrationTest).dependsOn(startMongo in IntegrationTest).andFinally {
        "docker rm -f $name_normalized$-it-mongo".!
      }
    }.value,

    fork in IntegrationTest := true,

    javaOptions in IntegrationTest := Seq(
      "-DMONGODB_URI=mongodb://localhost:53123/",
      "-Dorg.mongodb.async.type=netty"
    )
  )
}

lazy val buildSettings = Seq(
  packageName in Universal := name.value,
  mainClass in Compile := Some("$organization$.Server"),
  sources in (Compile, doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false,
  bashScriptExtraDefines += """addJava "-Dconfig.file=\${app_home}/../conf/application.conf"""",
  mappings in Universal += {
    val conf = (resourceDirectory in Compile).value / "application.conf"
    conf -> "conf/application.conf"
  }
)

lazy val wartRemoverSettings = Seq(
  wartremoverErrors in (Compile, compile) ++= Warts.allBut(
    Wart.Any,                 // false positives
    Wart.Nothing,             // false positives
    Wart.Product,             // false positives
    Wart.Serializable,        // false positives
    Wart.ImplicitParameter,   // only used for Pos, but evidently can't be suppressed
    Wart.ImplicitConversion,  // it's fine here
    Wart.PublicInference      // fails https://github.com/wartremover/wartremover/issues/398
  )
)

lazy val dockerSettings = Seq(
  dockerBaseImage := "openjdk:8-alpine",
  dockerUpdateLatest := true
)

lazy val releaseSettings = Seq(
  releaseVersionBump := sbtrelease.Version.Bump.Next,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    releaseStepTask(test in IntegrationTest),
    setReleaseVersion,
    releaseStepTask(publish in Docker),
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val commandSettings = Seq(
  addCommandAlias("scalafmtAll", ";scalafmt;test:scalafmt;it:scalafmt"),
  addCommandAlias("testAll", ";set coverageEnabled := true;clean;coverage;test;it:test;coverageReport")
).flatten
