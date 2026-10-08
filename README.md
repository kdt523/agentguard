# AgentGuard

AI agents get "tools" from MCP servers. A malicious or careless server can hide instructions in tool
descriptions (tool poisoning), silently change a tool after it was approved (rug pull), hijack another
server's tools (shadowing), or expose dangerous combinations of tools that let an attacker steal data
(toxic flows). AgentGuard connects to MCP servers, fetches their tools, runs static checks, builds a
capability graph to find toxic data-flow paths, pins tool definitions to detect rug pulls, red-teams a
simulated agent with LLM-generated attacks, and produces a TruRisk-inspired risk score with fixes, via a
REST API, a CLI (CI gate), and HTML/SARIF reports.

**Status: under construction.** Phases 0–1 complete (build skeleton, fixture MCP servers).

## Build

Requires JDK 21+ and Docker (for integration tests).

```bash
./mvnw verify                                        # unit + integration tests + coverage
docker compose -f deploy/compose.dev.yml up -d       # local Postgres
```

## License

MIT — see [LICENSE](LICENSE).
