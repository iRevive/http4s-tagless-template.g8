clone_repo() {
  echo "Cloning repo"

  REMOTE_BRANCH="update-$TRAVIS_COMMIT"

  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "Travis CI"

  git clone "https://${GH_TOKEN}@github.com/iRevive/http4s-tagless-example.git" . > /dev/null 2>&1

  git checkout -b "$REMOTE_BRANCH"
  git rm -r *
  git checkout HEAD -- .travis.yml || true
  git checkout HEAD -- README.md || true
  git commit -m "Clean up"

  pwd && ls -la && git status && git remote -v
}

push_changes() {
  echo "Pushing changes"

  pwd && ls -la && git status && git remote -v

  REMOTE_BRANCH="update-$TRAVIS_COMMIT"

  git add .*
  git add -A
  git rm -f "default.properties"
  git reset -- README.md
  git commit -m "$TRAVIS_COMMIT_MESSAGE"

  git push --force --quiet --set-upstream origin "$REMOTE_BRANCH"

  PR_URL="https://api.github.com/repos/iRevive/http4s-tagless-example/pulls"
  BODY="{\"title\":\"$TRAVIS_COMMIT_MESSAGE\",\"head\":\"$REMOTE_BRANCH\",\"base\":\"master\"}"

  echo "Branch $REMOTE_BRANCH"
  echo "Body $BODY"

  curl -s -H "Authorization: token $GH_TOKEN" -H "Content-Type: application/json" -d "$BODY" $PR_URL > /dev/null
}

perform_update() {
  cd ./..
  mkdir external-repository
  pwd
  cd external-repository
  clone_repo
  pwd
  cd ./..
  cp -R g8/. external-repository
  cd external-repository
  pwd
  push_changes
}

perform_update || true