## Http4s tagless template

[![Build & Test](https://github.com/iRevive/http4s-tagless-template.g8/actions/workflows/build.yml/badge.svg)](https://github.com/iRevive/http4s-tagless-template.g8/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/iRevive/http4s-tagless-template.g8/branch/master/graph/badge.svg)](https://codecov.io/gh/iRevive/http4s-tagless-template.g8)

Prerequisites:
- JDK 11
- Giter8 0.12.0
- SBT 1.3.11+

Open a console and run the following command to apply this template:
```
g8 https://github.com/iRevive/http4s-tagless-template.g8
 ```
or
```
sbt new iRevive/http4s-tagless-template.g8
```

## Template libraries

- [Http4s](https://github.com/http4s/http4s)
- [Monix](https://github.com/monix/monix)
- [Cats Effect](https://github.com/typelevel/cats-effect)
- [Cats MTL](https://github.com/typelevel/cats-mtl)
- [Doobie](https://github.com/tpolecat/doobie)

## Template configuration
This template will prompt for the following parameters. Press `Enter` if the default values suit you:
- `name`: Becomes the name of the project.
- `organization`: Specifies the organization for this project.
- `scala_version`: Specifies the Scala version for this project.

The template comes with the following sources:

* `GeneralApi.scala` - the class which handles requests.
* `PersistenceModuleResource.scala` - the class which has an initialization logic of PostgreSQL connector.
* `Server.scala` - the main class which starts up the HTTP server.
* `GeneralApiSpec.scala` - the class which tests routes.
* `PersistenceModuleResourceSpec.scala` - the class which has an integration test for a persistence module.
* `docker-compose.yml` - docker compose configuration. 
* `README.md` - the documentation with explanation of all project functions.

Once inside the project folder use the following command to run the code:
```
sbt clean test it:test run
```

## SBT plugins

#### [sbt-release](https://github.com/sbt/sbt-release)
The plugin configured without `publishArtifact` step. By default, it will publish a docker image.  

#### [sbt-native-packager](https://github.com/sbt/sbt-native-packager)
Almost default configuration.

#### [sbt-scoverage](https://github.com/scoverage/sbt-scoverage)
Default configuration without changes. Coverage disabled by default.

#### [sbt-scalafmt](https://github.com/scalameta/sbt-scalafmt)
Default configuration without changes. 

#### [wartremover](https://github.com/wartremover/wartremover)
Default configuration without changes.
 
## Integration tests

This template will generate a docker-based environment for integration tests.  
On the start of integration tests sbt will start PostgreSQL as a docker container, after tests it will be destroyed.
