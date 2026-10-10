#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
docker build -q -t agentguard/sandbox-node:dev -f "$DIR/Dockerfile.node" "$DIR"
docker build -q -t agentguard/sandbox-python:dev -f "$DIR/Dockerfile.python" "$DIR"
echo "sandbox images built"
