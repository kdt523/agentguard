package io.agentguard.core.model;

/** A resource advertised by an MCP server. */
public record ResourceDefinition(String uri, String name, String description, String mimeType) {

  public ResourceDefinition {
    description = description == null ? "" : description;
  }
}
