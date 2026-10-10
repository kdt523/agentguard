package io.agentguard.app.adapter.mcp;

import io.agentguard.core.model.PromptDefinition;
import io.agentguard.core.model.ResourceDefinition;
import io.agentguard.core.model.ToolAnnotations;
import io.agentguard.core.model.ToolDefinition;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Maps MCP Java SDK types to core records (Adapter); null descriptions become "". */
final class McpSchemaMapper {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private McpSchemaMapper() {}

  static ToolDefinition tool(McpSchema.Tool t) {
    JsonNode schema = t.inputSchema() == null ? null : MAPPER.valueToTree(t.inputSchema());
    return new ToolDefinition(
        t.name(), t.description(), schema, annotations(t.annotations()), null);
  }

  static ToolAnnotations annotations(McpSchema.ToolAnnotations a) {
    if (a == null) {
      return null;
    }
    return new ToolAnnotations(
        a.readOnlyHint(), a.destructiveHint(), a.idempotentHint(), a.openWorldHint(), a.title());
  }

  static PromptDefinition prompt(McpSchema.Prompt p) {
    return new PromptDefinition(p.name(), p.description());
  }

  static ResourceDefinition resource(McpSchema.Resource r) {
    return new ResourceDefinition(r.uri(), r.name(), r.description(), r.mimeType());
  }
}
