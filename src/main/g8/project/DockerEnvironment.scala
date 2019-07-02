object DockerEnvironment {

  object postgres {

    object container {
      val port          = 53124
      val user          = "postgres"
      val password      = "admin"
      val db            = "$name_normalized$"
      val containerName = "$name_normalized$-it-postgres"
      val image         = "postgres:9.6"

      def uri(network: String): String = network match {
        case "$name_normalized$-ci-network" => s"jdbc:postgresql://\$containerName:5432/\$db"
        case _                              => s"jdbc:postgresql://localhost:\$port/\$db"
      }
    }

    object commands {
      val remove: String = s"docker rm -f \${container.containerName}"

      def start(network: String): String =
        s"""
           |docker run --rm -d
           |-p \${container.port}:5432
           |--network=\$network
           |-e POSTGRES_USER=\${container.user} -e POSTGRES_PASSWORD=\${container.password} -e POSTGRES_DB=\${container.db}
           |--name \${container.containerName}
           |\${container.image}
         """.stripMargin.replace("\n", " ").trim
    }

  }

  object mongodb {

    object container {
      val port          = 53123
      val containerName = "$name_normalized$-it-mongo"
      val image         = "mongo"

      def uri(network: String): String = network match {
        case "$name_normalized$-ci-network" => s"mongodb://\$containerName:27017/?streamType=netty"
        case _                              => s"mongodb://localhost:\$port/?streamType=netty"
      }
    }

    object commands {
      val remove: String = s"docker rm -f \${container.containerName}"

      def start(network: String): String =
        s"docker run --rm -d -p \${container.port}:27017 --network=\$network --name \${container.containerName} \${container.image}"
    }

  }

}