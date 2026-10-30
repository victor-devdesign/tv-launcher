#!/usr/bin/env bash
# Builds the APK inside a container; nothing but Docker is needed on the host.
# Usage: ./build.sh [gradle task]   (default: assembleDebug; try assembleRelease)
set -e
cd "$(dirname "$0")"
IMG=tv-launcher-build
TASK=${1:-assembleDebug}

docker image inspect "$IMG" >/dev/null 2>&1 || docker build -t "$IMG" docker/build

docker run --rm \
  -v "$PWD":/project \
  -v tv-gradle-cache:/gradle-cache \
  -v tv-signing-keys:/root/.android \
  "$IMG" \
  sh -c "gradle --no-daemon $TASK; rc=\$?; chown -R $(id -u):$(id -g) /project/.gradle /project/build /project/app/build 2>/dev/null; exit \$rc"

find app/build/outputs/apk -name '*.apk'
