lazy val root = (project in file("."))
  .enablePlugins(ScriptedPlugin)
  .settings(
    name                            := "http4s-tagless.g8",
    scriptedLaunchOpts             ++= List("-Xms1024m", "-Xmx1024m", "-XX:ReservedCodeCacheSize=128m", "-Xss2m", "-Dfile.encoding=UTF-8"),
    scriptedBufferLog               := false,
    scriptedBufferLog in (Test, g8) := false,
    resolvers                       += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  )
