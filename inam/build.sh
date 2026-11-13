#!/usr/bin/env bash
#
# Fork addition (branch "inam"): builds the structurizr-inam Docker image from source.
# The version comes from inam/VERSION and is used as both the app version (startup banner) and the image tag.
#
# Usage:
#   inam/build.sh                                  # image for this machine's architecture, loaded into Docker
#   PLATFORMS=linux/amd64,linux/arm64 inam/build.sh  # multi-arch (needs a buildx builder and PUSH=1 with IMAGE set to a registry)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$REPO_ROOT/inam/VERSION")"
IMAGE="${IMAGE:-structurizr-inam}"

cd "$REPO_ROOT"

if [ -n "${PLATFORMS:-}" ]; then
    output="--load"
    if [ "${PUSH:-0}" = "1" ]; then
        output="--push"
    fi
    docker buildx build \
        --platform "$PLATFORMS" \
        -f inam/Dockerfile \
        --build-arg APP_REVISION="$VERSION" \
        -t "$IMAGE:$VERSION" \
        "$output" \
        .
else
    docker build \
        -f inam/Dockerfile \
        --build-arg APP_REVISION="$VERSION" \
        -t "$IMAGE:$VERSION" \
        .
fi

echo "Built $IMAGE:$VERSION"
