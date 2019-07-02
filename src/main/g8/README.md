# $name;format="cap"$: Developer's Guide

This document is both a development guideline and current
state of things description. It evolves together
with the project and is constantly a subject for changes.

> **Table of contents:**  
> - [Project structure](#project-structure)  
> - [Development requirements](#development-requirements)  
> - [Environment installation](#environment-installation)  
>   - [How to install sbt](#how-to-install-sbt)  
>   - [How to install docker](#how-to-install-docker)  
> - [Application configuration](#application-configuration)  
>   - [Configuration details](#configuration-details)  
> - [Tests execution](#tests-execution)  
>   - [How to run tests](#how-to-run-tests)  
>   - [How to run integration tests](#how-to-run-integration-tests)  
>   - [How to calculate test coverage](#how-to-calculate-test-coverage)  
> - [Docker configuration](#docker-configuration)  
>   - [Hot to create a CI image](#how-to-create-a-ci-image)
>   - [How to create a latest docker image](#how-to-create-a-latest-docker-image)  
>   - [How to run a dockerized application](#how-to-run-a-dockerized-application)  
> - [Deploy](#deploy)  
>   - [How to build a standalone application](#how-to-make-a-package)  
>   - [How to run an application](#how-to-run-an-application)  
>   - [How to release a version](#how-to-release-a-version)  


## <a name="project-structure"></a> Project structure
1) [src/main/scala/.../api](/src/main/scala/$organization;format="packaged"$/api) - API endpoints;  
2) [src/main/scala/.../persistence](/src/main/scala/$organization;format="packaged"$/persistence) - persistence-specific logic (mongo client, AMQP client, etc);    
3) [src/main/scala/.../processing](/src/main/scala/$organization;format="packaged"$/processing) - processing-specific logic (services, etc);  
5) [src/main/scala/.../util](/src/main/scala/$organization;format="packaged"$/util) - utility classes;  
6) [src/main/scala/.../Server.scala](/src/main/scala/$organization;format="packaged"$/Server.scala) - application entry point; 
7) [src/main/resources](/src/main/resources) - application and logback configs;    

## <a name="development-requirements"></a> Development requirements
1) Scala $scala_version$;  
2) SBT $sbt_version$;  
3) Docker;

## <a name="environment-installation"></a> Environment installation

#### <a name="how-to-install-sbt"></a> How to install sbt
1) [MacOS](http://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Mac.html)  
2) [Windows](http://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Windows.html)   
3) [Linux](http://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html)

#### <a name="how-to-install-docker"></a> How to install docker
Detailed instruction is [here](https://docs.docker.com/install/)   



## <a name="application-configuration"></a> Application configuration 

#### <a name="configuration-details"></a> Configuration details
All settings are stored in the `application.conf`.  
1) `application.http.host` - http host;  
2) `application.http.port` - http port;  
3) `LOG_LEVEL` - specify an environment variable to change a log level.  
By default, application is listening on port 9001.  



## <a name="tests-execution"></a> Tests execution

#### <a name="how-to-run-tests"></a> How to run tests
In a `<root>` project directory write in a console  
```
sbt test
```

#### <a name="how-to-run-integration-tests"></a> How to run integration tests
**Note** You should have installed docker.
In a `<root>` project directory write in a console  
```
sbt it:test
```

#### <a name="how-to-calculate-test-coverage"></a> How to calculate coverage
```
sbt 'set coverageEnabled := true' clean coverage test it:test coverageReport
```
or 
```
sbt testAll
```
Coverage reports will be in `target/scala-2.12/scoverage-report`. There are HTML and XML reports. The XML is useful if you need to programatically use the results, or if you're writing a tool.  
 
 
 
## <a name="docker-configuration"></a> Docker configuration 

#### <a name="how-to-create-a-ci-image"></a> How to create a CI image
SBT will publish an image locally using 'latest' tag.  
Execute in a `<root>` project folder:  
```
docker build -f docker/dockerfiles/sbt/Dockerfile . -t $name_normalized$/ci-sbt:latest
```

#### <a name="how-to-create-a-latest-docker-image"></a> How to create a latest docker image
SBT will publish an image locally using 'latest' tag.  
Execute in a `<root>` project folder:  
```
sbt docker:publishLocal
```

#### <a name="how-to-run-a-dockerized-application"></a> How to run a dockerized application
Make sure that you released a docker image with the latest or dev version.  
Check configuration properties in the `docker-compose.yml` file.  
Execute in a `<root>/docker` project folder: 
```
docker-compose up
```

## <a name="deploy"></a> Deploy

#### <a name="how-to-make-a-package"></a> How to build a standalone application
In a `<root>` project folder write in a console  
```
sbt universal:packageBin
```

The output file will be located at this place:
```
<root>/target/universal/$name_normalized$.zip
```

#### <a name="how-to-run-an-application"></a> How to run an application
Unzip an `$name_normalized$.zip` archive in any directory (`<root>/dist`, for example).    
Execute in a `<root>/dist/$name_normalized$` folder:    
```
sh bin/$name_normalized$
```

#### <a name="how-to-release-a-version"></a> How to release a version
The docker version (latest + release version) will be published as well.  
Execute in a `<root>` project folder:  
```
sbt "release with-defaults"
```
