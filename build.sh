#!/usr/bin/env bash
# check for all required executables
if ! [ -x "$(command -v git)" ]; then
  echo 'Error: git is not installed.' >&2
  exit 1
fi
if ! [ -x "$(command -v docker)" ]; then
  echo 'Error: docker is not installed.' >&2
  exit 1
fi
if ! [ -x "$(command -v mvn)" ]; then
  echo 'Error: maven is not installed.' >&2
  exit 1
fi
if ! [ -x "$(command -v java)" ]; then
  echo 'Error: java is not installed.' >&2
  exit 1
fi
# defaults
REGISTRY="ghcr.io/deb4sh"
# get current tag information
IS_DEV_BUILD=$(git tag -l --contains HEAD)
GIT_TAG=$(git describe --abbrev=0 --tags HEAD)

if [ -z "$IS_DEV_BUILD" ]
then
    TIMESTAMP=$(date +%s)
    TAG=$(echo "$GIT_TAG"-"$TIMESTAMP")
else
    TAG=$GIT_TAG
fi
# build yart
mvn clean package -f pom.xml
cp target/yart-0.0.0-SNAPSHOT-jar-with-dependencies.jar src/docker/yart.jar
docker build ./src/docker -f src/docker/Dockerfile -t $(echo "$REGISTRY/yart:$TAG")