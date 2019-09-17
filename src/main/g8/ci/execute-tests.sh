#!/usr/bin/env bash

if [ -z "\$1" ]; then
  echo "CI SBT docker image name is missing"
  exit 1
fi

sbt_image=\$1

set -euf pipefail

echo "Pulling CI SBT image \$sbt_image"
docker pull \$sbt_image || true

echo "Executing tests"

docker network rm $name_normalized$-ci-network || true
docker network create -d bridge $name_normalized$-ci-network

docker run --rm \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --mount src="\$(pwd)",target=/opt/workspace,type=bind \
    --network=$name_normalized$-ci-network \
    -e DOCKER_NETWORK=$name_normalized$-ci-network \
    \$sbt_image \
    sbt ci