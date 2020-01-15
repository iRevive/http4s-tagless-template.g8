# Database

The database connection can be configured by the configuration file or by the environment variables.  


| Config path                                    | Env variable             | Default value                             | Description                                          |
|------------------------------------------------|--------------------------|-------------------------------------------|------------------------------------------------------|
| application.persistence.postgres.uri           | POSTGRESQL_URI           | jdbc:postgresql://localhost:5432/postgres | The database uri                                     |
| application.persistence.postgres.user          | POSTGRESQL_USER          | root                                      | The database user                                    |
| application.persistence.postgres.password      | POSTGRESQL_PASSWORD      | root                                      | The database password                                |
| application.persistence.postgres.run-migration | POSTGRESQL_RUN_MIGRATION | true                                      | If true, execute Flyway migration during the startup |