import sbt.Keys.javaOptions
import sbtrelease.ReleaseStateTransformations._

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, AshScriptPlugin)
  $if(useMongo.truthy)$
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings))
  .settings(itEnvironment)
  $endif$
  .settings(commonSettings)
  .settings(testSettings)
  .settings(buildSettings)
  .settings(dockerSettings)
  .settings(releaseSettings)
  .settings(commandSettings)

lazy val commonSettings = Seq(
  organization := Settings.organization,
  name := Settings.name,
  scalaVersion := Versions.scala,
  scalacOptions ++= commonOptions ++ warnOptions ++ lintOptions,
  resolvers ++= Seq(Resolver.mavenLocal, Resolver.sbtPluginRepo("releases")),
  libraryDependencies ++= Dependencies.root,
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
)

lazy val testSettings = Seq(
  fork in Test := true,
  parallelExecution in Test := true,
  javaOptions in Test := Seq(
    "-Dorg.mongodb.async.type=netty"
  )
)

$if(useMongo.truthy)$
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
$endif$

lazy val buildSettings = Seq(
  packageName in Universal := name.value,
  mainClass in Compile := Some("$organization$.Server"),
  sources in (Compile, doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false,
  bashScriptExtraDefines += """addJava "-Dconfig.file=\${app_home}/../conf/application.conf"""",
  wartremoverErrors in (Compile, compile) ++= Warts.allBut(
    Wart.Any,                 // false positives
    Wart.Nothing,             // false positives
    Wart.Product,             // false positives
    Wart.Serializable,        // false positives
    Wart.ImplicitParameter,   // only used for Pos, but evidently can't be suppressed
    Wart.ImplicitConversion,  // it's fine here
    Wart.PublicInference      // fails https://github.com/wartremover/wartremover/issues/398
  ),
  mappings in Universal += {
    val conf = (resourceDirectory in Compile).value / "application.conf"
    conf -> "conf/application.conf"
  }
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

lazy val commonOptions = Seq(
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-Ypartial-unification",             // Enable partial unification in type constructor inference
  "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfuture",                          // Turn on future language features.
  "-Yno-adapted-args"                  // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
)

lazy val warnOptions = Seq(
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-value-discard",              // Warn when non-Unit expression results are unused.
  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
  "-Ywarn-unused:params",              // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",            // Warn if a private member is unused.
  "-Ywarn-extra-implicit"              // Warn when more than one implicit parameter section is defined.
)

lazy val lintOptions = Seq(
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match",              // Pattern match may not be typesafe.
  "-Ywarn-infer-any"                   // Warn when a type argument is inferred to be `Any`.
)