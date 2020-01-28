#!/usr/bin/env bash

declare -r sbt_image="\$1"

if [ -z "\$sbt_image" ]; then
  echo "CI SBT docker image name is missing"
  exit 1
fi

set -euf pipefail

echo "Pulling CI SBT image \$sbt_image"
docker pull "\$sbt_image" || true

echo "Executing tests"

declare -r ci-network="$name_normalized$-ci-network"

docker network rm "\$ci-network" || true
docker network create -d bridge "\$ci-network"

docker run --rm \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --mount src="\$(pwd)",target=/opt/workspace,type=bind \
    --network="\$ci-network" \
    -e DOCKER_NETWORK="\$ci-network" \
    -e DOCKER_REGISTRY_IMAGE="\$CI_REGISTRY_IMAGE" \
    "\$sbt_image" \
    sbt ci