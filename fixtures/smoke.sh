#!/usr/bin/env bash
# Smoke test: list tools of every fixture over stdio (docker run -i, no network) and streamable HTTP.
# Also verifies the rugpull fixture mutates after 3 listings and sends tools/list_changed.
# Prereqs: docker; images built by build_all.sh. Creates fixtures/.venv with the MCP Python SDK.
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

if [[ ! -d .venv ]]; then
  python3 -m venv .venv 2>/dev/null || python -m venv .venv
  VENV_PY=$( [[ -x .venv/bin/python ]] && echo .venv/bin/python || echo .venv/Scripts/python )
  "$VENV_PY" -m pip install -q -r requirements-smoke.txt
fi
PY=$( [[ -x .venv/bin/python ]] && echo .venv/bin/python || echo .venv/Scripts/python )

PORT=18100
FAIL=0
printf '%-20s %-6s %-6s %s\n' FIXTURE TRANSP TOOLS RESULT
row() { printf '%-20s %-6s %-6s %s\n' "$1" "$2" "$3" "$4"; [[ "$4" == OK ]] || FAIL=1; }
count_of() { sed -n 's/.*"count": \([0-9]*\).*/\1/p'; }

wait_http() {
  for _ in $(seq 1 40); do
    curl -s -o /dev/null "http://localhost:$1/mcp" && return 0
    sleep 0.25
  done
  return 1
}

while read -r f; do
  [[ -z "$f" ]] && continue
  img="agentguard/fixture-$f:dev"

  out=$("$PY" smoke_client.py stdio "$img" 2>/dev/null); n=$(echo "$out" | count_of)
  [[ -n "$n" && "$n" -gt 0 ]] && row "$f" stdio "$n" OK || row "$f" stdio "-" FAIL

  PORT=$((PORT + 1)); name="ag-smoke-$f"
  docker run -d --rm --name "$name" -p "$PORT:8000" "$img" --transport http >/dev/null
  if wait_http "$PORT"; then
    out=$("$PY" smoke_client.py http "http://localhost:$PORT/mcp" 2>/dev/null); n=$(echo "$out" | count_of)
    [[ -n "$n" && "$n" -gt 0 ]] && row "$f" http "$n" OK || row "$f" http "-" FAIL
    if [[ "$f" == rugpull ]]; then
      docker rm -f "$name" >/dev/null; PORT=$((PORT + 1))
      docker run -d --rm --name "$name" -p "$PORT:8000" "$img" --transport http >/dev/null
      wait_http "$PORT"
      out=$("$PY" smoke_client.py rugpull-http "http://localhost:$PORT/mcp" 2>/dev/null)
      [[ "$out" == *'"last_poisoned": true'*'"list_changed": true'* ]] && row "$f" mutate "-" OK || row "$f" mutate "-" "FAIL $out"
    fi
  else
    row "$f" http "-" "FAIL (no listen)"
  fi
  docker rm -f "$name" >/dev/null 2>&1
done < fixtures.txt

[[ $FAIL -eq 0 ]] && echo "smoke: ALL OK" || echo "smoke: FAILURES"
exit $FAIL
