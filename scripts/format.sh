#!/usr/bin/env bash
# Formats all Java sources with google-java-format (standalone jar; works on any JDK >= 21).
# On JDK 21-25 you can equally run: ./mvnw spotless:apply
set -euo pipefail
GJF_VERSION=1.37.0
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/.mvn/tools/google-java-format-$GJF_VERSION.jar"
if [[ ! -f "$JAR" ]]; then
  mkdir -p "$(dirname "$JAR")"
  curl -sSfLo "$JAR" "https://repo1.maven.org/maven2/com/google/googlejavaformat/google-java-format/$GJF_VERSION/google-java-format-$GJF_VERSION-all-deps.jar"
fi
find "$ROOT" -path '*/target' -prune -o -name '*.java' -print0 | xargs -0 java -jar "$JAR" --replace
echo "formatted"
