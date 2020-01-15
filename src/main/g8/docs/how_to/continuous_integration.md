# Continuous integration

- [Overview](#overview)  
- [How to create a CI image](#how-to-create-a-ci-image)  
- [Semi-automated release](#semi-automated-release)  

## <a name="overview"></a> Overview

CI uses custom docker image (`ci-sbt`) for compilation of the project. The image has all necessary dependencies: 
sbt, docker, docker-compose, and cached jar dependencies. More info in [Dockerfile](../../docker/dockerfiles/sbt/Dockerfile).

## <a name="how-to-create-a-ci-image"></a> How to create a CI image

Execute in a `<root>` project folder:  
```
docker build -f docker/dockerfiles/sbt/Dockerfile . -t $name_normalized$/ci-sbt:latest
```

## <a name="semi-automated-release"></a> Semi-automated release

GitLab CI can make a release only from the `release` branch.   

Release steps:
1) Execute unit and integration tests;
2) Increment snapshot version to major version (e.g. 0.0.1-SNAPSHOT -> 0.0.1);
3) Create a git tag with a corresponding major version;
4) Create new docker image and push two tags (`0.0.1` and `latest`) to a remote registry: 
5) Set new snapshot version (0.0.2-SNAPSHOT);
6) Push changes to the `release` branch;

Once the changes were pushed to the `release` branch, the release step can be triggered manually.