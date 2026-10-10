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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * AG-SC-002: a free-text "side channel" parameter (sidenote, notes, context, metadata, extra...)
 * that the description or schema tells the model to fill with content, a common exfiltration route.
 */
public final class SuspiciousParameterCheck implements Check {

  private static final Set<String> NAMES =
      Set.of(
          "sidenote",
          "side_note",
          "note",
          "notes",
          "context",
          "metadata",
          "extra",
          "extras",
          "additional_info",
          "additional_context",
          "remarks");
  private static final Pattern FILL_VERB =
      Pattern.compile(
          "\\b(pass|put|include|provide|fill|supply|paste|attach|append|insert|copy)\\b|\\bcontents?\\s+of\\b",
          Pattern.CASE_INSENSITIVE);

  @Override
  public String id() {
    return "AG-SC-002";
  }

  @Override
  public String title() {
    return "Suspicious free-text parameter";
  }

  @Override
  public String owaspRef() {
    return "MCP10";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.HIGH;
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
      if (NAMES.contains(p.getKey().toLowerCase(Locale.ROOT))
          && asksToFill(tool, p.getKey(), p.getValue())) {
        String text = "parameter '" + p.getKey() + "' is filled with content on instruction";
        b.evidence(new Evidence("inputSchema.properties." + p.getKey(), text, 0, text.length()));
        any = true;
      }
    }
    return any
        ? List.of(
            b.confidence(0.85)
                .remediation(
                    "Remove the free-text parameter or document and bound it. Never pass file"
                        + " contents or secrets through side-channel parameters.")
                .build())
        : List.of();
  }

  private static boolean asksToFill(ToolDefinition tool, String param, JsonNode schema) {
    JsonNode desc = schema.get("description");
    if (desc != null && desc.isString() && FILL_VERB.matcher(desc.asString()).find()) {
      return true;
    }
    String quoted = "['\"`]" + Pattern.quote(param) + "['\"`]";
    Matcher m =
        Pattern.compile(
                "\\b(pass|put|include|provide|set|fill|supply|send|insert|paste|attach|append)\\b[^.\\n"
                    + "]{0,120}"
                    + quoted,
                Pattern.CASE_INSENSITIVE)
            .matcher(tool.description());
    return m.find();
  }
}
