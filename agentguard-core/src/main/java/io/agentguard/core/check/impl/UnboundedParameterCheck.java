package io.agentguard.core.check.impl;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.FindingBuilder;
import io.agentguard.core.check.Scope;
import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** AG-PE-004: path, URL or command parameters with no pattern, enum, format or maxLength. */
public final class UnboundedParameterCheck implements Check {

  private static final Set<String> RISKY_LAST_TOKEN =
      Set.of(
          "path",
          "file",
          "filename",
          "filepath",
          "dir",
          "directory",
          "folder",
          "url",
          "uri",
          "command",
          "cmd",
          "script");
  private static final List<String> BOUNDS =
      List.of("pattern", "enum", "format", "maxLength", "const");

  @Override
  public String id() {
    return "AG-PE-004";
  }

  @Override
  public String title() {
    return "Unbounded path, URL or command parameter";
  }

  @Override
  public String owaspRef() {
    return "MCP05";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.LOW;
  }

  @Override
  public Scope scope() {
    return Scope.TOOL;
  }

  @Override
  public List<Finding> run(CheckContext ctx) {
    ToolDefinition tool = ctx.tool();
    if (tool == null) {
      return List.of();
    }
    FindingBuilder b = FindingBuilder.of(this, ctx);
    boolean any = false;
    for (Map.Entry<String, JsonNode> p : ToolText.params(tool).entrySet()) {
      if (isRisky(p.getKey()) && isStringLike(p.getValue()) && !isBounded(p.getValue())) {
        String text = "parameter '" + p.getKey() + "' accepts any string";
        b.evidence(new Evidence("inputSchema.properties." + p.getKey(), text, 0, text.length()));
        any = true;
      }
    }
    return any
        ? List.of(
            b.confidence(0.6)
                .remediation(
                    "Add a JSON-schema pattern, enum, format or maxLength to constrain the"
                        + " parameter and validate it server-side (allow-list paths and hosts).")
                .build())
        : List.of();
  }

  private static boolean isRisky(String name) {
    List<String> tokens = ToolText.nameTokens(name);
    return !tokens.isEmpty() && RISKY_LAST_TOKEN.contains(tokens.get(tokens.size() - 1));
  }

  private static boolean isStringLike(JsonNode schema) {
    JsonNode type = schema.get("type");
    return type == null || (type.isString() && type.asString().equals("string"));
  }

  private static boolean isBounded(JsonNode schema) {
    return BOUNDS.stream().anyMatch(schema::has);
  }
}
