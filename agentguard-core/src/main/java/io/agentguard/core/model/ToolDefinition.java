package io.agentguard.core.model;

import java.util.Objects;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/** A tool as advertised by an MCP server, with its canonical sha256. */
public record ToolDefinition(
    String name,
    String description,
    JsonNode inputSchema,
    ToolAnnotations annotations,
    String sha256) {

  public ToolDefinition {
    Objects.requireNonNull(name, "name");
    description = description == null ? "" : description;
    inputSchema = inputSchema == null ? JsonNodeFactory.instance.objectNode() : inputSchema;
    sha256 = sha256 == null ? "" : sha256;
  }

  /** Annotations, empty when the server sent none. */
  public Optional<ToolAnnotations> annotationsOpt() {
    return Optional.ofNullable(annotations);
  }
}
