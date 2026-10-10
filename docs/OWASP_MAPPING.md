# OWASP mapping

Source: OWASP MCP Top 10, 2025 edition (github.com/OWASP/www-project-mcp-top-10), fetched 2026-10-10.
The list is a "living document" and carries no version number.

| ID | Official name |
|---|---|
| MCP01:2025 | Token Mismanagement & Secret Exposure |
| MCP02:2025 | Privilege Escalation via Scope Creep |
| MCP03:2025 | Tool Poisoning |
| MCP04:2025 | Software Supply Chain Attacks & Dependency Tampering |
| MCP05:2025 | Command Injection & Execution |
| MCP06:2025 | Prompt Injection via Contextual Payloads |
| MCP07:2025 | Insufficient Authentication & Authorization |
| MCP08:2025 | Lack of Audit and Telemetry |
| MCP09:2025 | Shadow MCP Servers |
| MCP10:2025 | Context Injection & Over-Sharing |

## AgentGuard check → OWASP MCP mapping

| Check | Title | `owaspRef` | Why |
|---|---|---|---|
| AG-TP-001 | Hidden instruction block | MCP03 | Tool poisoning |
| AG-TP-002 | Secrecy / user deception | MCP03 | Tool poisoning |
| AG-TP-003 | Sensitive resource reference | MCP01 | Instructs access to secrets/credentials |
| AG-TP-004 | Invisible / confusable characters | MCP03 | Hidden poisoning |
| AG-TP-005 | Encoded payload | MCP03 | Hidden poisoning |
| AG-TP-006 | Excessive description length | MCP03 | Common carrier for poisoning |
| AG-SH-001 | Cross-tool reference | MCP03 | Shadowing is a tool-poisoning variant (no dedicated entry) |
| AG-SH-002 | Behaviour override | MCP03 | Same |
| AG-SH-003 | Tool name collision | MCP03 | Same |
| AG-PE-001 | Code execution capability | MCP05 | Command injection & execution |
| AG-PE-002 | Destructive without hint | MCP02 | Scope creep / excessive agency |
| AG-PE-003 | Missing annotations | MCP02 | Same |
| AG-PE-004 | Unbounded parameter | MCP05 | Unvalidated path/url/command input |
| AG-SC-001 | Instruction in schema | MCP03 | Poisoning outside the description |
| AG-SC-002 | Suspicious free-text parameter | MCP10 | Context injection / over-sharing channel |
| AG-CF-001 | Unpinned package launch | MCP04 | Supply chain |
| AG-CF-002 | Secret in config | MCP01 | Secret exposure |
| AG-CF-003 | Insecure remote transport | MCP07 | Missing transport authentication/integrity |
| AG-DR-* (phase 5) | Rug pull / drift | MCP03 | Tool changed after approval; also MCP04 |
| AG-TF-* (phase 4) | Toxic flow | MCP06 | Prompt injection leading to exfiltration; also MCP10 |

Notes:
- The OWASP MCP list has no dedicated entries for shadowing or rug pulls; they are mapped to the closest
  entry (MCP03) and called out as variants. See `docs/DECISIONS.md`.
- The OWASP LLM Top 10 (2025) overlaps as: LLM01 Prompt Injection (TP-*, SC-001, TF-*),
  LLM03 Supply Chain (CF-001), LLM06 Excessive Agency (PE-*). These are informational, not stored in findings.
