# AgentGuard architecture

> Placeholder — finalised in phase 10.

Hexagonal (ports & adapters):

- `agentguard-core` — pure Java 21 domain model and algorithms (checks, capability tagging, toxic-flow
  graph, hashing/diff, scoring). No framework dependencies (enforced by ArchUnit).
- `agentguard-app` — Spring Boot: REST API, MCP client adapter, Docker sandbox, persistence (Postgres +
  Flyway), LLM adapter (Spring AI), scheduler.
- `agentguard-cli` — picocli CLI usable as a CI gate.

Ports live in `io.agentguard.core.port`: `McpSnapshotPort`, `LlmPort`, `SnapshotRepositoryPort`,
`PinRepositoryPort`, `ClockPort`.
