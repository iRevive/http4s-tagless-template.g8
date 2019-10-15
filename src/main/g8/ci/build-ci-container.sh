#!/usr/bin/env bash

if [ -z "\$1" ]; then
  echo "CI SBT docker image name is missing"
  exit 1
fi

sbt_image=\$1

set -ef pipefail

echo "Pulling CI SBT image \$sbt_image"
docker pull \$sbt_image || true

if [ -n "\$(docker images -q \$sbt_image)" ] && [ -z "\$REBUILD_CI_CONTAINER" ]; then
  echo "SBT container already exists"
  exit
fi

echo "Creating SBT container"

docker build \
    --pull \
    --file ./docker/dockerfiles/sbt/Dockerfile \
    --cache-from \$sbt_image:latest \
    --tag \$sbt_image \
    .

docker push \$sbt_image