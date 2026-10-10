# Decisions

One entry per non-obvious decision: date, decision, why, alternatives considered.

## 2026-10-09 — Dependency versions (verified on Maven Central)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.1 | Version Spring AI 2.0.1's auto-configure modules are built against |
| Spring AI (BOM) | 2.0.1 | Latest stable (2.1.0-M1 is a milestone) |
| MCP Java SDK `io.modelcontextprotocol.sdk:mcp` | 2.0.0 | Version pinned by `spring-ai-mcp` 2.0.1 (2.0.1 exists; we follow Spring AI's pin) |
| Jackson | 3.1.5 (`tools.jackson.*`) | Managed by Boot 4.1.1; used by MCP SDK 2.x and Spring AI 2.x |
| Testcontainers BOM | 2.0.5 | Module names `testcontainers-postgresql`, `testcontainers-junit-jupiter` |
| ArchUnit | 1.5.1 | |
| jqwik | 1.10.1 | |
| picocli | 4.7.7 | |
| springdoc-openapi | 3.1.1 | Line for Boot 4 |
| WireMock | 3.13.2 | 4.x still beta |
| JaCoCo plugin | 0.8.15 | |
| Spotless / google-java-format | 3.10.3 / 1.37.0 | |
| Maven | 3.9.16 (wrapper 3.3.4) | |
| Flyway | 12.4.0 (Boot-managed) | Boot 4 needs `spring-boot-starter-flyway` |

## 2026-10-09 — Jackson 3 in core
- **Decision:** `agentguard-core` depends on `tools.jackson.core:jackson-databind` (Jackson 3), so
  `ToolDefinition.inputSchema` is a `tools.jackson.databind.JsonNode`.
- **Why:** Boot 4, Spring AI 2 and MCP SDK 2 are all on Jackson 3; using Jackson 2 in core would force
  conversions at every adapter boundary and two Jackson stacks on the classpath.
- **Alternatives:** Jackson 2 (`com.fasterxml`) — rejected for the reasons above.

## 2026-10-09 — Gemini via `spring-ai-starter-model-google-genai`
- **Decision:** use Spring AI's Google GenAI starter (Gemini Developer API with an API key), not the
  Vertex AI Gemini starter. LLM auto-config is off by default (`spring.ai.model.chat=${AG_LLM_PROVIDER:none}`)
  so the app starts without a key; phase 7 enables it.
- **Alternatives:** `spring-ai-starter-model-vertex-ai-gemini` — needs a GCP project and service
  account, not the free-tier API key.

## 2026-10-09 — Plain MCP SDK instead of Spring AI MCP client starter
- **Decision:** depend on `io.modelcontextprotocol.sdk:mcp` directly and build clients ourselves in
  `McpClientAdapter`.
- **Why:** AgentGuard connects to arbitrary, user-supplied servers per scan; the starter auto-configures
  a fixed set of clients from properties at startup, which is the wrong lifecycle.

## 2026-10-09 — Native Windows dev environment, JDK 26 locally
- **Decision:** develop on native Windows (Git Bash for scripts, Docker Desktop) with the locally
  installed Temurin 26; compile with `--release 21`. CI runs Temurin 21.
- **Why:** the machine has no WSL Ubuntu distribution and no JDK 21; `--release 21` guarantees the
  bytecode/API level is Java 21.
- **Consequences:** surefire/failsafe pass `-Dnet.bytebuddy.experimental=true` so Mockito works on JDK 26.

## 2026-10-09 — google-java-format check only on JDK 21–25
- **Decision:** `spotless:check` lives in profile `format-check`, auto-activated on JDK `[21,26)`.
  Locally (JDK 26) code is formatted with `scripts/format.sh` (standalone google-java-format jar).
- **Why:** Spotless runs google-java-format in-process and it fails with an opaque
  `InvocationTargetException` on JDK 26, while the same formatter version works standalone. CI on
  JDK 21 still enforces formatting on every push.
- **Alternatives:** drop formatting enforcement (rejected); pin JDK 21 locally (not installed).

## 2026-10-09 — Repository location
- **Decision:** the repo root is the project folder; build plans are kept locally, outside git.
  `START_HERE.md` and `.claude/` were copied to the root. The original kit folder is gitignored.

## 2026-10-09 — Fixtures use MCP Python SDK 2.3.0 (`MCPServer`, not `FastMCP`)
- **Decision:** fixtures pin `mcp==2.3.0`. In SDK v2 `FastMCP` was renamed to
  `mcp.server.mcpserver.MCPServer`; the skill's "FastMCP" maps to that class.
- The v2 server still accepts the classic initialize handshake (2024-11-05 … 2025-11-25) in addition to
  the 2026-07-28 protocol, so older clients (incl. the Java SDK) can connect.

## 2026-10-09 — Fixture ground-truth refinements
- `shadowing-mailer` has **no** `<IMPORTANT>` tags, so it plants only AG-SH-001/002 (with the tags,
  AG-TP-001 would rightly fire and muddy the ground truth).
- `overprivileged-fs` expectations are split per tool in `expected.yaml`: `run_shell` → PE-001..004,
  `delete_path` → PE-002..004, `read_file` → PE-004.
- Trifecta and clean tools carry explicit annotations and bounded (`pattern`/`maxLength`) params so
  they plant no static issues — they exist only for the toxic-flow graph.
- `rugpull` sends `notifications/tools/list_changed` **request-scoped** (on the response stream of the
  triggering `tools/list`): a connection-scoped notification was not delivered to a streamable-HTTP
  client that had not opened the standalone SSE stream.

## 2026-10-09 — Smoke client is the MCP Python SDK, in `fixtures/.venv`
- **Decision:** `fixtures/smoke.sh` uses `smoke_client.py` (MCP Python SDK client) instead of
  `@modelcontextprotocol/inspector --cli`, avoiding a Node dependency. stdio fixtures are launched with
  `docker run --rm -i --network none`.

## 2026-10-10 — Phase 2: MCP connector decisions
- **MCP Java SDK 2.0.0 API (verified with javap):** `McpClient.sync(transport)`, `StdioClientTransport(ServerParameters, McpJsonDefaults.getMapper())`,
  `HttpClientStreamableHttpTransport.builder(origin).endpoint(path)`, `HttpClientSseClientTransport.builder(origin).sseEndpoint(path)`.
  Tool schemas arrive as `Map<String,Object>`; the adapter converts them to Jackson 3 `JsonNode`.
- **Sandbox install step:** `SandboxCommandBuilder.install()` (bridge network, writes into a per-server
  named volume `agentguard-sbx-cache-<key>`) is implemented and unit-tested; `run()` then mounts the same
  volume with `--network none` and offline env (`npm_config_offline`, `UV_OFFLINE`). Trade-off: arbitrary
  packages cannot be baked into an image, so the one-time install phase has network access but no secrets
  (env is only passed to the offline run). The install flow is only wired for real third-party servers in
  phase 8, after the STOP-point confirmation of `benchmarks/clean_servers.yaml`.
- **Remote third-party servers** must match `agentguard.remote.allowed-hosts`; otherwise rejected.
- **Dedup:** an unchanged `snapshotSha256` only advances `fetched_at`; env values are never persisted
  (`servers.config` stores `envKeys` only).
- **Pagination** of list calls is capped at 100 pages so a malicious server cannot loop forever.
- **ITs need Docker Desktop running**; they fail with "Could not find a valid Docker environment" otherwise.

## 2026-10-10 — Phase 3: static check engine decisions
- **Taxonomy:** OWASP MCP Top 10 (2025) verified at github.com/OWASP/www-project-mcp-top-10; mapping and
  rationale in `docs/OWASP_MAPPING.md`. The list has no dedicated shadowing or rug-pull entries, so
  AG-SH-* and AG-DR-* map to MCP03 (Tool Poisoning) as variants.
- **Pattern files are line-based, not YAML:** `agentguard-core` may only depend on Jackson databind, so
  `patterns/<id>.patterns` use `confidence | name | regex` lines. (YAML is used only in core *tests*.)
- **Hidden text is judged once, by the check that owns it:** TP-001/TP-002/SC-001 inspect *visible*
  text only. Text hidden via the Unicode tag block is reported by TP-004 (escalated to CRITICAL when the
  decoded text is an instruction) and base64/hex/percent blobs by TP-005. This keeps one planted issue
  from producing four findings and matches `fixtures/expected.yaml`.
- **Scope of text checks:** TP-001/002/003 and SH-001/002 read the tool's own description/title; SC-001 reads
  schema text (description, default, enum, examples, title). A poisoned `default` is therefore SC-001, not TP-001.
- **PE-002 verb list includes `run`/`exec`/`execute`** so shell runners are flagged as destructive without
  `destructiveHint` (required by the `overprivileged-fs` ground truth). `add` is deliberately not a side-effect verb.
- **SH-001 only matches identifier-like tool names** (snake/kebab/camelCase, >= 4 chars) of tools on a
  *different* server, so plain words such as `add` never cause false positives.
- **SH-003 and the fixtures:** `add` (calculator + poisoned-math) and `read_file` (overprivileged-fs +
  trifecta-secrets) legitimately collide when all fixtures are scanned together. `expected.yaml` therefore
  has a `cross_server` section; per-server scans must match `servers:` exactly and the combined scan must
  equal `servers:` + `cross_server:`.
- **Unknown servers are treated as third-party** for scoring (+5), the conservative default.
- **A crashing check never aborts a scan:** it becomes an INFO finding `AG-ENG-000` (message redacted).
- **Emoji ZWJ sequences and LRM/RLM in RTL text are not flagged** by TP-004, to avoid false positives on
  legitimate text.
- **LLM classifier (AG-TP-900) is implemented but not wired:** `LlmPoisoningClassifier` + `CachingLlmPort`
  exist and are unit-tested with a fake LLM; no `LlmPort` adapter exists until phase 7, and scan
  integration is phase 6. Tool text is fenced as untrusted data in the prompt.
- **Known limitations:** config-level checks (CF-*) run per *successfully fetched* server, so a server that
  fails to connect gets no config findings; evidence offsets refer to the analysed view, not the raw field.
