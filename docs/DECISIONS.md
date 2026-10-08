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
- **Decision:** the repo root is `c:\agentguard`; the blueprint kit's `CLAUDE.md`, `PLAN.md`,
  `START_HERE.md` and `.claude/` were copied to the root. The original kit folder is gitignored.
