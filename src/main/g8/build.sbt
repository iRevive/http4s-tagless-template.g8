import sbtrelease.ReleaseStateTransformations._

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging, AshScriptPlugin, IntegrationEnvPlugin)
  .configs(IntegrationTest)
  .settings(commonSettings)
  .settings(scalazDerivingSettings)
  .settings(wartRemoverSettings)
  .settings(testSettings)
  .settings(integrationTestSettings)
  .settings(buildSettings)
  .settings(dockerSettings)
  .settings(releaseSettings)
  .settings(commandSettings)
  .settings(
    name                 := Settings.name,
    libraryDependencies ++= Dependencies.root
  )

lazy val docs = project
  .in(file(s"\${Settings.name}-docs"))
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .settings(commonSettings)
  .settings(
    moduleName := s"\${Settings.name}-docs",
    mdocVariables := Map(
      "REPO_URL" -> sys.env.getOrElse("REPO_URL", "")
    )
  )
  .dependsOn(root)

lazy val commonSettings = Seq(
  organization  := Settings.organization,
  scalaVersion  := Versions.scala,
  javacOptions ++= Seq("-source", "11"),
  scalacOptions += "-Ymacro-annotations",
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % Versions.kindProjector cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % Versions.betterMonadicFor)
)

lazy val scalazDerivingSettings = Seq(
  // WORKAROUND for scalaz.annotation.deriving: https://github.com/sbt/sbt/issues/1965
  Compile / managedClasspath := {
    val res = (Compile / resourceDirectory).value
    val old = (Compile / managedClasspath).value
    Attributed.blank(res) +: old
  },
  addCompilerPlugin("org.scalaz" %% "deriving-plugin" % Versions.scalazDeriving cross CrossVersion.full)
)

lazy val testSettings = Seq(
  Test / fork              := true,
  Test / parallelExecution := true
)

lazy val integrationTestSettings = Seq(
  IntegrationTest / testOptions       := integrationEnvTestOpts.value,
  IntegrationTest / fork              := true,
  IntegrationTest / parallelExecution := false,
  IntegrationTest / javaOptions := {
    val mode = integrationEnvMode.value

    val postgreUri = mode.fold(
      "jdbc:postgresql://postgres:5432/$name_normalized$",
      "jdbc:postgresql://localhost:55432/$name_normalized$"
    )

    val (postgreUser, postgrePassword) = ("postgres", "admin")

    Seq(
      s"-DPOSTGRESQL_URI=\$postgreUri",
      s"-DPOSTGRESQL_USER=\$postgreUser",
      s"-DPOSTGRESQL_PASSWORD=\$postgrePassword"
    )
  },
  integrationEnvProvider := {
    val projectName        = name.value
    val composeProjectName = s"\$projectName-it-env"
    val dockerComposeFile  = sourceDirectory.value / "it" / "docker" / "docker-compose.yml"
    val network            = sys.env.get("DOCKER_NETWORK").orElse(Some(s"\$projectName-network"))

    new DockerComposeEnvProvider(composeProjectName, dockerComposeFile, network)
  }
) ++ Defaults.itSettings ++ inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings)

lazy val buildSettings = Seq(
  Universal / packageName                := name.value,
  Compile / mainClass                    := Some("$organization$.Server"),
  Compile / doc / sources                := Seq.empty,
  Compile / packageDoc / publishArtifact := false,
  bashScriptExtraDefines                 += """addJava "-Dconfig.file=\${app_home}/../conf/application.conf"""",
  Universal / mappings += {
    val conf = (Compile / resourceDirectory).value / "application.conf"
    conf -> "conf/application.conf"
  }
)

lazy val wartRemoverSettings = Seq(
  Compile / compile / wartremoverErrors ++= Warts.allBut(
    Wart.Any,                // false positives
    Wart.Nothing,            // false positives
    Wart.Product,            // false positives
    Wart.Serializable,       // false positives
    Wart.ImplicitParameter,  // it's fine here
    Wart.ImplicitConversion, // it's fine here
    Wart.PublicInference     // fails https://github.com/wartremover/wartremover/issues/398
  )
)

lazy val dockerSettings = Seq(
  dockerBaseImage    := "adoptopenjdk/openjdk11:alpine",
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
  val ci = Command.command("ci") { state =>
    "clean" ::
      "coverage" ::
      "scalafmtSbtCheck" ::
      "scalafmtCheckAll" ::
      "test:compile" ::
      "it:compile" ::
      "test" ::
      "it:test" ::
      "coverageReport" ::
      state
  }

  val testAll =
    Command.command("testAll")(state => "clean" :: "coverage" :: "test" :: "it:test" :: "coverageReport" :: state)

  commands ++= List(ci, testAll)
}
