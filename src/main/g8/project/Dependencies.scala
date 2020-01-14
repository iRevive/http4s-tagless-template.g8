import sbt._

object Settings {
  val organization = "$organization$"
  val name         = "$name_normalized$"
}

object Versions {
  val scala               = "$scala_version$"
  val http4s              = "$http4s_version$"
  val circe               = "$circe_version$"
  val circeExtras         = "$circe_extras_version$"
  val circeConfig         = "$circe_config_version$"
  val cats                = "$cats_version$"
  val catsEffect          = "$cats_effect_version$"
  val catsMTL             = "$cats_mtl_version$"
  val catsRetry           = "$cats_retry_version$"
  val monix               = "$monix_version$"
  val sup                 = "$sup_version$"
  val doobie              = "$doobie_version$"
  val flyway              = "$flyway_version$"
  val newtype             = "$newtype_version$"
  val refined             = "$refined_version$"
  val magnolia            = "$magnolia_version$"
  val odin                = "$odin_version$"
  val logback             = "$logback_version$"
  val scalatest           = "$scalatest_version$"
  val scalatestScalacheck = "$scalatest_scalacheck_version$"
  val catsScalatest       = "$cats_scalatest_version$"
  val catsEffectScalaTest = "$cats_effect_scalatest_version$"
  val scalacheck          = "$scalacheck_version$"
  val scalazDeriving      = "$scalaz_deriving_version$"
  val kindProjector       = "$kind_projector_version$"
  val betterMonadicFor    = "$better_monadic_for_version$"
}

object Dependencies {

  val CatsEffectScalaTest = "cats-effect-testing-scalatest-scalacheck"

  val root: Seq[ModuleID] = Seq(
    "org.http4s"           %% "http4s-dsl"           % Versions.http4s,
    "org.http4s"           %% "http4s-blaze-server"  % Versions.http4s,
    "org.http4s"           %% "http4s-circe"         % Versions.http4s,
    "io.monix"             %% "monix"                % Versions.monix,
    "org.typelevel"        %% "cats-core"            % Versions.cats,
    "org.typelevel"        %% "cats-effect"          % Versions.catsEffect,
    "org.typelevel"        %% "cats-mtl-core"        % Versions.catsMTL,
    "com.github.cb372"     %% "cats-retry-mtl"       % Versions.catsRetry,
    "org.scalaz"           %% "deriving-macro"       % Versions.scalazDeriving,
    "io.circe"             %% "circe-generic"        % Versions.circe,
    "io.circe"             %% "circe-refined"        % Versions.circe,
    "io.circe"             %% "circe-generic-extras" % Versions.circeExtras,
    "io.circe"             %% "circe-config"         % Versions.circeConfig,
    "io.estatico"          %% "newtype"              % Versions.newtype,
    "eu.timepit"           %% "refined"              % Versions.refined,
    "eu.timepit"           %% "refined-cats"         % Versions.refined,
    "com.kubukoz"          %% "sup-doobie"           % Versions.sup,
    "com.kubukoz"          %% "sup-http4s"           % Versions.sup,
    "com.kubukoz"          %% "sup-circe"            % Versions.sup,
    "org.tpolecat"         %% "doobie-hikari"        % Versions.doobie,
    "org.tpolecat"         %% "doobie-refined"       % Versions.doobie,
    "org.tpolecat"         %% "doobie-postgres"      % Versions.doobie,
    "org.flywaydb"         % "flyway-core"           % Versions.flyway,
    "com.propensive"       %% "magnolia"             % Versions.magnolia,
    "com.github.valskalla" %% "odin-extras"          % Versions.odin,
    "ch.qos.logback"       % "logback-classic"       % Versions.logback,
    "org.tpolecat"         %% "doobie-scalatest"     % Versions.doobie % "it",
    "org.scalatest"        %% "scalatest"            % Versions.scalatest % "it,test",
    "org.scalatestplus"    %% "scalacheck-1-14"      % Versions.scalatestScalacheck % "it,test",
    "com.ironcorelabs"     %% "cats-scalatest"       % Versions.catsScalatest % "it,test",
    "com.codecommit"       %% CatsEffectScalaTest    % Versions.catsEffectScalaTest % "it,test",
    "org.scalacheck"       %% "scalacheck"           % Versions.scalacheck % "it,test",
    "eu.timepit"           %% "refined-scalacheck"   % Versions.refined % "it,test"
  )

}
