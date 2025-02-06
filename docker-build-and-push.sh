#! /bin/bash
mvn clean install;
# docker buildx create --name container --driver=docker-container
# used https://medium.com/@life-is-short-so-enjoy-it/docker-how-to-build-and-push-multi-arch-docker-images-to-docker-hub-64dea4931df9
docker buildx build \
 --tag clemhen/iis-sandbox:deployable-v1.3 \
 --platform linux/arm64/v8,linux/amd64 \
 --builder container \
 --push .
