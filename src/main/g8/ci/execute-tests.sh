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

echo "Executing tests and verification of formatting"

docker network rm $name_normalized$-ci-network || true
docker network create -d bridge $name_normalized$-ci-network

docker run --rm \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --mount src="\$(pwd)",target=/opt/workspace,type=bind \
    --network=$name_normalized$-ci-network \
    -e DOCKER_NETWORK=$name_normalized$-ci-network \
    \$sbt_image \
    sbt ci