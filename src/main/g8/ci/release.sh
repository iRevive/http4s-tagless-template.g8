#!/usr/bin/env bash

if [ "\$#" -ne 1 ]; then
  echo "Docker image name is missing"
  exit 1
fi

docker_image=\$1
sbt_image="\$docker_image/ci-sbt:latest"

set -euf pipefail

echo "Pulling CI SBT image"
docker pull \$sbt_image || true

echo "Releasing application"
echo "SBT image \$sbt_image"

docker run --rm \
     -v /var/run/docker.sock:/var/run/docker.sock \
     --mount src="\$(pwd)",target=/opt/workspace,type=bind \
     -e DOCKER_REGISTRY_IMAGE=\$docker_image \
     \$sbt_image \
     sbt "release with-defaults"
