#!/usr/bin/env bash
#
# Fork addition (branch "inam"): runs the structurizr-inam image in server mode on this machine.
#
# - Listens on 127.0.0.1 only: open-core has no login (structurizr.authentication=none), so the
#   server must not be reachable from the network until an authentication extension exists.
# - Workspace data lives outside both repositories, in $DATA_DIR (default ~/structurizr-data).
#
# Usage:
#   inam/run-server.sh          # start (replaces a previous container with the same name)
#   docker logs -f structurizr-server
#   docker rm -f structurizr-server

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$REPO_ROOT/inam/VERSION")"
IMAGE="${IMAGE:-structurizr-inam}"
NAME="${NAME:-structurizr-server}"
PORT="${PORT:-8081}"
DATA_DIR="${DATA_DIR:-$HOME/structurizr-data}"

mkdir -p "$DATA_DIR"

docker rm -f "$NAME" >/dev/null 2>&1 || true

docker run -d \
    --name "$NAME" \
    --restart unless-stopped \
    -p "127.0.0.1:$PORT:8080" \
    -v "$DATA_DIR":/usr/local/structurizr \
    "$IMAGE:$VERSION" \
    server

echo "Structurizr server $VERSION: http://localhost:$PORT  (data: $DATA_DIR)"
