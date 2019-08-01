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
    name                := Settings.name,
    libraryDependencies ++= Dependencies.root
  )

lazy val commonSettings = Seq(
  organization  := Settings.organization,
  scalaVersion  := Versions.scala,
  scalacOptions -= "-Xfatal-warnings",
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.10"),
  addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.0")
)

lazy val scalazDerivingSettings = Seq(
  // WORKAROUND for scalaz.deriving: https://github.com/sbt/sbt/issues/1965
  Compile / managedClasspath := {
    val res = (Compile / resourceDirectory).value
    val old = (Compile / managedClasspath).value
    Attributed.blank(res) +: old
  },
  addCompilerPlugin("org.scalaz" %% "deriving-plugin" % Versions.scalazDeriving)
)

lazy val testSettings = Seq(
  Test / fork              := true,
  Test / parallelExecution := true,
  Test / javaOptions       := Seq("-Dorg.mongodb.async.type=netty")
)

lazy val itEnvironment = {
  val startItEnv = TaskKey[Unit]("start-it-env", "Create integration environment")
  val stopItEnv  = TaskKey[Unit]("stop-it-env", "Destroy integration environment")
  val env        = DockerEnvironment.createEnv(sys.env.get("DOCKER_NETWORK"))

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

      val cleanup = Tests.Setup { () =>
        log.info("Destroying integration environment")
        env.destroy(sourceDir)
      }

      Seq(setup, cleanup)
    }.value,
    IntegrationTest / fork              := true,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / javaOptions       := env.javaOpts
  )
}

lazy val buildSettings = Seq(
  Universal / packageName                := name.value,
  Compile / mainClass                    := Some("$organization$.Server"),
  Compile / doc / sources                := Seq.empty,
  Compile / packageDoc / publishArtifact := false,
  bashScriptExtraDefines                 += """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
  Universal / mappings += {
    val conf = (Compile / resourceDirectory).value / "application.conf"
    conf -> "conf/application.conf"
  }
)

lazy val wartRemoverSettings = Seq(
  Compile / compile / wartremoverErrors ++= Warts.allBut(
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
  dockerBaseImage    := "openjdk:8-alpine",
  dockerUpdateLatest := true,
  dockerAlias := DockerAlias(
    None,
    None,
    sys.env.getOrElse("DOCKER_REGISTRY_IMAGE", name.value),
    Option((Docker / version).value)
  )
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

lazy val commandSettings = {
  val ciSteps = List(
    "clean",
    "coverage",
    "scalafmtCheck",
    "test:scalafmtCheck",
    "it:scalafmtCheck",
    "test:compile",
    "it:compile",
    "test",
    "it:test",
    "coverageReport"
  )

  Seq(
    addCommandAlias("scalafmtAll", ";scalafmt;test:scalafmt;it:scalafmt"),
    addCommandAlias("testAll", ";clean;coverage;test;it:test;coverageReport"),
    addCommandAlias("ci", ciSteps.mkString(";", ";", ""))
  ).flatten
}
