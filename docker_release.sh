#!/bin/bash
set -e

RELEASE_VERSION=$1
NEW_VERSION=$2

git checkout "Baragon-$RELEASE_VERSION"
mvn clean package docker:build -DskipTests

git checkout master
mvn clean package docker:build -DskipTests

docker tag hubspot/baragonagent-aurora:$NEW_VERSION hubspot/baragonagent-aurora:latest
docker tag hubspot/baragonagent:$NEW_VERSION hubspot/baragonagent:latest
docker tag hubspot/baragonservice:$NEW_VERSION hubspot/baragonservice:latest
docker push hubspot/baragonagent-aurora:$RELEASE_VERSION && docker push hubspot/baragonagent-aurora:$NEW_VERSION && docker push hubspot/baragonagent-aurora:latest && docker push hubspot/baragonagent:$RELEASE_VERSION && docker push hubspot/baragonagent:$NEW_VERSION && docker push hubspot/baragonagent:latest && docker push hubspot/baragonservice:$RELEASE_VERSION && docker push hubspot/baragonservice:$NEW_VERSION && docker push hubspot/baragonservice:latest