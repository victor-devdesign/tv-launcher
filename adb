#!/usr/bin/env bash
# Wrapper: runs adb inside a persistent container (keeps keys + server alive).
set -e
NAME=tv-adb
IMG=tv-launcher-adb
cd "$(dirname "$0")"
if ! docker image inspect "$IMG" >/dev/null 2>&1; then
  docker build -q -t "$IMG" docker/adb >/dev/null
fi
if [ "$(docker inspect -f '{{.State.Running}}' "$NAME" 2>/dev/null)" != "true" ]; then
  docker rm -f "$NAME" >/dev/null 2>&1 || true
  docker run -d --name "$NAME" -v tv-adbkeys:/root/.android -v "$PWD":/work -w /work "$IMG" >/dev/null
fi
exec docker exec -i "$NAME" adb "$@"
