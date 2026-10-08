#!/usr/bin/env bash
# Builds every fixture image as agentguard/fixture-<name>:dev
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
while read -r f; do
  [[ -z "$f" ]] && continue
  echo "==> building agentguard/fixture-$f:dev"
  docker build -q -t "agentguard/fixture-$f:dev" -f "$DIR/$f/Dockerfile" "$DIR" >/dev/null
done < "$DIR/fixtures.txt"
echo "all fixture images built"
