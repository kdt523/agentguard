package io.agentguard.core.check.impl;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.FindingBuilder;
import io.agentguard.core.check.Scope;
import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolAnnotations;
import io.agentguard.core.model.ToolDefinition;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** AG-PE-003: a tool with side-effect verbs declares no annotations at all. */
public final class MissingAnnotationsCheck implements Check {

  private static final Set<String> SIDE_EFFECT_VERBS = sideEffectVerbs();

  private static Set<String> sideEffectVerbs() {
    Set<String> s = new HashSet<>(DestructiveWithoutHintCheck.DESTRUCTIVE_VERBS);
    s.addAll(
        Set.of(
            "send", "post", "create", "upload", "publish", "push", "commit", "deploy", "insert",
            "modify", "edit", "append", "install"));
    return Set.copyOf(s);
  }

  @Override
  public String id() {
    return "AG-PE-003";
  }

  @Override
  public String title() {
    return "Side-effect tool without annotations";
  }

  @Override
  public String owaspRef() {
    return "MCP02";
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
    if (tool == null || hasAnnotations(tool.annotations())) {
      return List.of();
    }
    String verb = ToolText.findVerb(tool, SIDE_EFFECT_VERBS);
    if (verb == null) {
      return List.of();
    }
    return List.of(
        FindingBuilder.of(this, ctx)
            .confidence(0.7)
            .evidence(
                new Evidence(
                    "annotations", "no annotations on a tool that can '" + verb + "'", 0, 0))
            .remediation(
                "Declare readOnlyHint, destructiveHint, idempotentHint and openWorldHint so clients"
                    + " can apply the right confirmation policy.")
            .build());
  }

  private static boolean hasAnnotations(ToolAnnotations a) {
    return a != null
        && (a.readOnlyHint() != null
            || a.destructiveHint() != null
            || a.idempotentHint() != null
            || a.openWorldHint() != null);
  }
}
