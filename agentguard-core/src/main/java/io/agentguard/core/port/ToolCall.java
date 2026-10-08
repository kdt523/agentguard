package io.agentguard.core.port;

/** A tool call requested by the model. */
public record ToolCall(String toolName, String argumentsJson) {}
