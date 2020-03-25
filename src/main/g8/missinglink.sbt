inThisBuild(
  Seq(
    missinglinkExcludedDependencies ++= Seq(
      moduleFilter(organization = "ch.qos.logback"),
      moduleFilter(organization = "org.flywaydb")
    ),
    missinglinkIgnoreDestinationPackages ++= Seq(
      IgnoredPackage("com.codahale"),
      IgnoredPackage("org.codehaus"),
      IgnoredPackage("io.micrometer"),
      IgnoredPackage("groovy.lang")
    ),
    missinglinkIgnoreSourcePackages ++= Seq(
      IgnoredPackage("org.flywaydb"),
      IgnoredPackage("ch.qos.logback")
    )
  )
)
