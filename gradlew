#!/bin/sh
set -eu
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DIST="$ROOT/.gradle-bootstrap/gradle-8.13"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$ROOT/.gradle-bootstrap"
  curl -fL "https://services.gradle.org/distributions/gradle-8.13-bin.zip" -o "$ROOT/.gradle-bootstrap/gradle.zip"
  unzip -q "$ROOT/.gradle-bootstrap/gradle.zip" -d "$ROOT/.gradle-bootstrap"
  rm "$ROOT/.gradle-bootstrap/gradle.zip"
fi
exec "$DIST/bin/gradle" "$@"
