#!/usr/bin/env bash

if [ -z "\$1" ]; then
  echo "CI SBT docker image name is missing"
  exit 1
fi

sbt_image=\$1

set -euf pipefail

echo "Pulling CI SBT image \$sbt_image"
docker pull \$sbt_image || true

echo "Releasing application"

docker network rm scala-gitlab-ci-network || true
docker network create -d bridge scala-gitlab-ci-network

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
    --network=scala-gitlab-ci-network \
    -e DOCKER_NETWORK=scala-gitlab-ci-network \
    -e DOCKER_REGISTRY_IMAGE=\$CI_REGISTRY_IMAGE \
    \$sbt_image \
    bash ./ci/release_entrypoint.sh
