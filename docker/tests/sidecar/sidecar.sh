printUsage () {
  echo ""
  echo "Usage:"
  echo '-d      database: (postgres as default) -> One of ["postgres", "mysql", "oracle", "mssql"]'
  echo '-b      branch: (current branch as default)'
  echo '-e      extra parameters: Must be send inside quotes "'
  echo '-r      [no arguments] run only: Will not executed a build of the image, use the -r option if an image was already generated'
  echo '-c      [no arguments] cache: allows to use the docker cache otherwise "--no-cache" will be use when building the image'
  echo ""
  echo "============================================================================"
  echo "============================================================================"
  echo "         Examples:"
  echo ""
  echo "         ./sidecar.sh"
  echo "         ./sidecar.sh -r"
  echo "         ./sidecar.sh -c"
  echo "         ./sidecar.sh -d mysql"
  echo "         ./sidecar.sh -d mysql -b origin/master"
  echo "         ./sidecar.sh -d mysql -b myBranchName"
  echo '         ./sidecar.sh -e "--debug-jvm"'
  echo '         ./sidecar.sh -e "--tests *HTMLPageAssetRenderedTest"'
  echo '         ./sidecar.sh -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"'
  echo "============================================================================"
  echo "============================================================================"
  echo ""
  echo "Note:"
  echo "[postgres] as default database if -d option is not use."
  echo "If -b option is not use current branch will be use."
  echo "By default we build the image using the --no-cache option, if cache is required the -c option should be use."
}

buildImage=true
useCache=false

while getopts "d:b:e:g:rch" option; do
  case ${option} in

  d) database=${OPTARG} ;;
  b) branchOrCommit=${OPTARG} ;;
  g) gitHash=${OPTARG} ;;
  e) extra=${OPTARG} ;;
  r) buildImage=false ;;
  c) useCache=true ;;
  h) printUsage
    exit 1
    ;;
  *)
    printUsage
    exit 1
    ;;
  esac
done

echo ""
BUILD_IMAGE_TAG="dotcms-sidecar-image"

#  One of ["postgres", "mysql", "oracle", "mssql"]
if [ -z "${database}" ]; then
  echo " >>> database parameter NOT FOUND, setting [postgres] as default DB"
  database=postgres
fi

if [ -z "${gitHash}" ]; then
  gitHash=$(git rev-parse HEAD)
  echo " >>> git hash parameter NOT FOUND, using current git commit hash [${gitHash}]"
fi

if [ -z "${branchOrCommit}" ]; then

  foundBranchName=$(git symbolic-ref -q HEAD)
  foundBranchName=${foundBranchName##refs/heads/}
  foundBranchName=${foundBranchName:-HEAD}

  echo " >>> Branch or commit parameter NOT FOUND, using current branch [${foundBranchName}]"
  branchOrCommit=${foundBranchName}
fi
echo ""

# Creating required folders
mkdir -p output

if [ "$buildImage" = true ]; then

  # Building the docker image for a given branch
  if [ "$useCache" = true ]; then
    docker build --pull \
      --build-arg BUILD_FROM=COMMIT \
      --build-arg BUILD_ID=${branchOrCommit} \
      --build-arg BUILD_HASH=${gitHash} \
      --build-arg LICENSE_KEY=${LICENSE_KEY} \
      -t ${BUILD_IMAGE_TAG} .
  else
    docker build --pull --no-cache \
      --build-arg BUILD_FROM=COMMIT \
      --build-arg BUILD_ID=${branchOrCommit} \
      --build-arg BUILD_HASH=${gitHash} \
      --build-arg LICENSE_KEY=${LICENSE_KEY} \
      -t ${BUILD_IMAGE_TAG} .
  fi

fi

if [ ! -z "${extra}" ]; then
  echo ""
  echo " >>> Running with extra parameters [${extra}]"
  echo ""
  export EXTRA_PARAMS=${extra}
fi

# Starting the container for the build image
export databaseType=${database}
export IMAGE_BASE_NAME=${BUILD_IMAGE_TAG}
docker-compose -f sidecar-service-compose.yml \
  -f ../shared/${database}-docker-compose.yml \
  -f ../shared/open-distro-docker-compose.yml \
  up \
  --abort-on-container-exit

# Cleaning up
docker-compose -f sidecar-service-compose.yml \
  -f ../shared/${database}-docker-compose.yml \
  -f ../shared/open-distro-docker-compose.yml \
  down
