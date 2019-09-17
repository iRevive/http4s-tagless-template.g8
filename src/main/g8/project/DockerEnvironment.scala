import sbt._
import scala.sys.process._

object DockerEnvironment {

  val DefaultNetwork: String = "$name_normalized$-network"
  val ComposeProject: String = "$name_normalized$".replace("-", "_")

  def createEnv(network: Option[String]): DockerEnv = new DockerEnv(network.getOrElse(DefaultNetwork))

  class DockerEnv(network: String) {

    def start(sourceDirectory: File): Unit = {
      val path = dockerComposePath(sourceDirectory)

      createNetwork()
      Process(s"docker-compose -f \$path -p \$ComposeProject up -d", None, dockerEnvVars: _*).!
    }

    def destroy(sourceDirectory: File): Unit = {
      val path = dockerComposePath(sourceDirectory)
      Process(s"docker-compose -f \$path -p \$ComposeProject down", None, dockerEnvVars: _*).!
    }

    def javaOpts: Seq[String] = {
      val postgreUri = foldNetwork(
        "jdbc:postgresql://localhost:55432/$name_normalized$",
        "jdbc:postgresql://postgres:5432/$name_normalized$"
      )

      val (postgreUser, postgrePassword) = ("postgres", "admin")

      val mongoUri = foldNetwork(
        "mongodb://localhost:57017/?streamType=netty",
        "mongodb://mongo:27017/?streamType=netty"
      )

      Seq(
        s"-DMONGODB_URI=\$mongoUri",
        "-Dorg.mongodb.async.type=netty",
        s"-DPOSTGRESQL_URI=\$postgreUri",
        s"-DPOSTGRESQL_USER=\$postgreUser",
        s"-DPOSTGRESQL_PASSWORD=\$postgrePassword"
      )
    }

    private def createNetwork(): Unit =
      s"docker network create \$network".!

    private def dockerComposePath(sourceDirectory: File): File =
      sourceDirectory / "it" / "docker" / "docker-compose.yml"

    private def dockerEnvVars: Seq[(String, String)] = Seq(("NETWORK", network))

    private def foldNetwork[A](onDefault: => A, onExternal: => A): A =
      if (network == DefaultNetwork) onDefault else onExternal

  }

}
