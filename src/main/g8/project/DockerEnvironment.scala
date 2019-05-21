object DockerEnvironment {

  object postgres {

    object container {
      val port          = 53124
      val user          = "postgres"
      val password      = "admin"
      val db            = "$name_normalized$"
      val containerName = "$name_normalized$-it-postgres"
      val image         = "postgres:9.6"
    }

    object commands {
      val remove = s"docker rm -f \${container.containerName}"

      val start: String =
        s"""
           |docker run --rm -d
           |-p \${container.port}:5432
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
    }

    object commands {
      val remove = s"docker rm -f \${container.containerName}"

      val start: String =
        s"docker run --rm -d -p \${container.port}:27017 --name \${container.containerName} \${container.image}"
    }

  }

}