package io.agentguard.core.model;

/** A prompt template advertised by an MCP server. */
public record PromptDefinition(String name, String description) {

  public PromptDefinition {
    description = description == null ? "" : description;
  }
}
