#!/usr/bin/env bash

if [ "\$#" -ne 1 ]; then
  echo "Docker image name is missing"
  exit 1
fi

docker_image=\$1
sbt_image="\$docker_image/ci-sbt:latest"

set -euf pipefail

echo "Pulling CI SBT image \$sbt_image"
docker pull \$sbt_image || true

if test ! -z "\$(docker images -q \$sbt_image)"; then
  echo "SBT container already exists"
  exit
fi


echo "Creating SBT container"

docker build -f ./docker/dockerfiles/sbt/Dockerfile . -t \$sbt_image
docker push \$sbt_image