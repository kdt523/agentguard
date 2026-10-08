package io.agentguard.core.model;

/** MCP tool annotation hints; any field may be absent (null), as in the protocol. */
public record ToolAnnotations(
    Boolean readOnlyHint,
    Boolean destructiveHint,
    Boolean idempotentHint,
    Boolean openWorldHint,
    String title) {}
