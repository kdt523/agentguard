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
import java.util.Set;

/**
 * AG-PE-002: the tool name/description implies a destructive action but destructiveHint is not
 * true.
 */
public final class DestructiveWithoutHintCheck implements Check {

  /** Verbs that change or destroy state; shared with the missing-annotations check. */
  static final Set<String> DESTRUCTIVE_VERBS =
      Set.of(
          "delete",
          "drop",
          "remove",
          "transfer",
          "write",
          "update",
          "run",
          "exec",
          "execute",
          "kill",
          "truncate",
          "overwrite",
          "destroy",
          "wipe",
          "erase",
          "purge",
          "rm");

  @Override
  public String id() {
    return "AG-PE-002";
  }

  @Override
  public String title() {
    return "Destructive tool without destructiveHint";
  }

  @Override
  public String owaspRef() {
    return "MCP02";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.MEDIUM;
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
    String verb = ToolText.findVerb(tool, DESTRUCTIVE_VERBS);
    boolean hinted =
        tool.annotationsOpt().map(a -> Boolean.TRUE.equals(a.destructiveHint())).orElse(false);
    if (verb == null || hinted) {
      return List.of();
    }
    return List.of(
        FindingBuilder.of(this, ctx)
            .confidence(0.7)
            .evidence(
                new Evidence(
                    "name", "action '" + verb + "' without destructiveHint=true", 0, verb.length()))
            .remediation(
                "Set annotations.destructiveHint=true so clients ask for confirmation before the"
                    + " call.")
            .build());
  }
}
