import sbt._

object Settings {
  val organization = "$organization$"
  val name         = "$name_normalized$"
}

object Versions {
  val scala           = "$scala_version$"
  val http4s          = "$http4s_version$"
  val circe           = "$circe_version$"
  val cats            = "$cats_version$"
  val catsEffect      = "$cats_effect_version$"
  val catsMTL         = "$cats_mtl_version$"
  val catsRetry       = "$cats_retry_version$"
  val sup             = "$sup_version$"
  val pureconfig      = "$pureconfig_version$"
  val doobie          = "$doobie_version$"
  val flyway          = "$flyway_version$"
  val newtype         = "$newtype_version$"
  val refined         = "$refined_version$"
  val odin            = "$odin_version$"
  val logback         = "$logback_version$"
  val unionDerivation = "$union_derivation_version$"
  val weaverTest      = "$weaver_test_version$"
}

object Dependencies {

  val root: Seq[ModuleID] = Seq(
    "org.http4s"            %% "http4s-dsl"            % Versions.http4s,
    "org.http4s"            %% "http4s-blaze-server"   % Versions.http4s,
    "org.http4s"            %% "http4s-circe"          % Versions.http4s,
    "org.typelevel"         %% "cats-core"             % Versions.cats,
    "org.typelevel"         %% "cats-effect"           % Versions.catsEffect,
    "org.typelevel"         %% "cats-mtl"              % Versions.catsMTL,
    "com.github.cb372"      %% "cats-retry-mtl"        % Versions.catsRetry,
    "io.circe"              %% "circe-generic"         % Versions.circe,
    "io.circe"              %% "circe-refined"         % Versions.circe,
    "com.github.pureconfig" %% "pureconfig-core"       % Versions.pureconfig,
    "io.monix"              %% "newtypes-core"         % Versions.newtype,
    "eu.timepit"            %% "refined"               % Versions.refined,
    "com.kubukoz"           %% "sup-doobie"            % Versions.sup,
    "com.kubukoz"           %% "sup-http4s"            % Versions.sup,
    "com.kubukoz"           %% "sup-circe"             % Versions.sup,
    "org.tpolecat"          %% "doobie-hikari"         % Versions.doobie,
    "org.tpolecat"          %% "doobie-refined"        % Versions.doobie,
    "org.tpolecat"          %% "doobie-postgres"       % Versions.doobie,
    "org.flywaydb"           % "flyway-core"           % Versions.flyway,
    "com.github.valskalla"  %% "odin-extras"           % Versions.odin,
    "ch.qos.logback"         % "logback-classic"       % Versions.logback,
    "io.github.irevive"     %% "union-derivation-core" % Versions.unionDerivation,
    "com.disneystreaming"   %% "weaver-cats"           % Versions.weaverTest % "test-shared,it,test",
    "com.disneystreaming"   %% "weaver-scalacheck"     % Versions.weaverTest % "test-shared,it,test",
    "eu.timepit"            %% "refined-scalacheck"    % Versions.refined    % "test-shared,it,test"
  )

}
