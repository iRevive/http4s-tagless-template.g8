---
id: run-dockerized-application
title: Run dockerized application
---

- [Create a docker image](#create-a-docker-image)  
- [Run a dockerized application](#run-a-dockerized-application)

## <a name="create-a-docker-image"></a> Create docker image

A docker image of the application can be created within sbt:
```sh
\$ sbt docker:publishLocal
```

SBT will publish an image with two tags: current version and `latest`.

## <a name="run-a-dockerized-application"></a> Run a dockerized application

Make sure that you released a docker image with the latest or dev version.  
Check configuration properties in the [docker-compose.yml](@REPO_URL@/docker/docker-compose.yml) file.  

Execute in the `<root>/docker` folder: 
```sh
\$ docker-compose up
```
