import sbt._

object Settings {
  val organization          = "$organization$"
  val name                  = "$name_normalized$"
}

object Versions {
  val scala             = "$scala_version$"

  val http4s            = "$http4s_version$"
  val circe             = "$circe_version$"
  val circeConfig       = "$circe_config_version$"
  val cats              = "$cats_version$"
  val catsEffect        = "$cats_effect_version$"
  val catsMTL           = "$cats_mtl_version$"
  val monix             = "$monix_version$"
  val mongoScalaDriver  = "$mongo_scala_driver_version$"
  val netty             = "$netty_version$"
  val doobie            = "$doobie_version$"
  val scalazDeriving    = "$scalaz_deriving_version$"
  val refined           = "$refined_version$"
  val sourcecode        = "$sourcecode_version$"
  val magnolia          = "$magnorlia_version$"
  val logback           = "$logback_version$"
  val scalaLogging      = "$scala_logging_version$"
  val scalatest         = "$scalatest_version$"
  val catsScalatest     = "$cats_scalatest_version$"
}

object Dependencies {

  val root: Seq[ModuleID] = Seq(
    "org.http4s"                 %% "http4s-dsl"           % Versions.http4s,
    "org.http4s"                 %% "http4s-blaze-server"  % Versions.http4s,
    "org.http4s"                 %% "http4s-circe"         % Versions.http4s,
    "io.monix"                   %% "monix"                % Versions.monix,
    "org.typelevel"              %% "cats-core"            % Versions.cats,
    "org.typelevel"              %% "cats-effect"          % Versions.catsEffect,
    "org.typelevel"              %% "cats-mtl-core"        % Versions.catsMTL,
    "org.scalaz"                 %% "deriving-macro"       % Versions.scalazDeriving,
    "io.circe"                   %% "circe-core"           % Versions.circe,
    "io.circe"                   %% "circe-jawn"           % Versions.circe,
    "io.circe"                   %% "circe-generic"        % Versions.circe,
    "io.circe"                   %% "circe-generic-extras" % Versions.circe,
    "io.circe"                   %% "circe-refined"        % Versions.circe,
    "io.circe"                   %% "circe-config"         % Versions.circeConfig,
    "eu.timepit"                 %% "refined"              % Versions.refined,
    "eu.timepit"                 %% "refined-cats"         % Versions.refined,
    "org.tpolecat"               %% "doobie-core"          % Versions.doobie,
    "org.tpolecat"               %% "doobie-hikari"        % Versions.doobie,
    "org.tpolecat"               %% "doobie-refined"       % Versions.doobie,
    "org.tpolecat"               %% "doobie-postgres"      % Versions.doobie,
    "org.mongodb.scala"          %% "mongo-scala-driver"   % Versions.mongoScalaDriver,
    "io.netty"                   %  "netty-all"            % Versions.netty,
    "com.lihaoyi"                %% "sourcecode"           % Versions.sourcecode,
    "com.propensive"             %% "magnolia"             % Versions.magnolia,
    "com.typesafe.scala-logging" %% "scala-logging"        % Versions.scalaLogging,
    "ch.qos.logback"             %  "logback-classic"      % Versions.logback,
    "org.tpolecat"               %% "doobie-scalatest"     % Versions.doobie        % "it",
    "org.scalatest"              %% "scalatest"            % Versions.scalatest     % "it,test",
    "com.ironcorelabs"           %% "cats-scalatest"       % Versions.catsScalatest % "it,test"
  )

}