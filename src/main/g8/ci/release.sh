#!/usr/bin/env bash

declare -r sbt_image="\$1"

if [ -z "\$sbt_image" ]; then
  echo "CI SBT docker image name is missing"
  exit 1
fi

set -euf pipefail

echo "Pulling CI SBT image \$sbt_image"
docker pull "\$sbt_image" || true

echo "Releasing application"

declare -r ci_network="$name_normalized$-ci-network"

docker network rm "\$ci_network" || true
docker network create -d bridge "\$ci_network"

# Prepare git
git config user.email "ci@gitlab.com"
git config user.name "CI Pipeline"
git checkout -B release
git branch --set-upstream-to=origin/release
git remote set-url origin "git@\$CI_SERVER_HOST:\$CI_PROJECT_PATH.git"

mkdir -p ./ci/shared

# Share SSH key with container
echo "\$SSH_PRIVATE_KEY" > ./ci/shared/ssh_private_key

# Share arguments for 'docker login' with container
echo "echo -n \$CI_JOB_TOKEN | docker login -u gitlab-ci-token --password-stdin \$CI_REGISTRY" > ./ci/shared/docker_login

docker run --rm \
    --user \$(id -u):\$(id -g) \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --mount src="\$(pwd)",target=/opt/workspace,type=bind \
    --network="\$ci_network" \
    -e DOCKER_NETWORK="\$ci_network" \
    -e DOCKER_REGISTRY_IMAGE="\$CI_REGISTRY_IMAGE" \
    "\$sbt_image" \
    bash ./ci/release_entrypoint.sh
