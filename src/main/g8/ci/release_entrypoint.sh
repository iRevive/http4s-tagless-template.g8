eval \$(cat ./ci/shared/docker_login) && \
  eval \$(ssh-agent -s) && \
  ssh-add <(cat ./ci/shared/ssh_private_key | dos2unix) && \
  mkdir -p ~/.ssh && \
  echo -e "Host *\n\tStrictHostKeyChecking no\n\n" > ~/.ssh/config && \
  sbt "release with-defaults"