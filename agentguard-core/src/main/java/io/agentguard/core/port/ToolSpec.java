package io.agentguard.core.port;

/** A tool offered to the LLM: name, description and JSON schema of its arguments. */
public record ToolSpec(String name, String description, String inputSchemaJson) {}
