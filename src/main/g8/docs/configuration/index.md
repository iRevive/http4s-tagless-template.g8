---
id: index
title: Configuration
---

- [HTTP server](http-server.md)
- [Database](database.md)

## Configuration via application.conf

Create a file `application.conf` and attach it to the container via docker-compose file:
```
services:
  api:
    image: $name_normalized$:latest
    ...
    volume:
      - ./application.conf:/opt/docker/conf/application.conf
```

Then append the file with the necessary settings:
```hocon
application {
  persistence {
    postgres {
      uri = "the-new-uri"
    }
  }
}
```

## Configuration via environment variables

Provide a necessary variable via docker-compose file: 
```
services:
  api:
    image: $name_normalized$:latest
    ...
    environment:
      - POSTGRESQL_URI=jdbc:postgresql://postgres:5432/postgres
```
