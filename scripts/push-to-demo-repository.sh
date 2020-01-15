update_remote_repo() {
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "Travis CI"

  git init

  REMOTE_BRANCH="update-$TRAVIS_COMMIT"

  git checkout -b REMOTE_BRANCH
  git remote add origin https://${GH_TOKEN}@github.com/iRevive/http4s-tagless-example.git > /dev/null 2>&1

  git add -A && git commit -m TRAVIS_COMMIT_MESSAGE
  git push --force --quiet --set-upstream origin REMOTE_BRANCH


  PR_URL="https://api.github.com/repos/iRevive/http4s-tagless-example/pulls"
  BODY="{\"title\":\"$TRAVIS_COMMIT_MESSAGE\",\"head\":\"$REMOTE_BRANCH,\"base\":\"master\"}"

  curl -s -H "Authorization: token $GH_TOKEN" -H "Content-Type: application/json" -d "$BODY" $PR_URL
}