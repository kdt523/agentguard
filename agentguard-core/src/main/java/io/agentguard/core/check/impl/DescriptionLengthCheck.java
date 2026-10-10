package io.agentguard.core.check.impl;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.FindingBuilder;
import io.agentguard.core.check.Scope;
import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolDefinition;
import java.util.Comparator;
import java.util.List;

/**
 * AG-TP-006: unusually long description, a common carrier for poisoning. Fires above an absolute
 * limit, or above a multiple of the server's median when the server has at least three tools.
 */
public final class DescriptionLengthCheck implements Check {

  @Override
  public String id() {
    return "AG-TP-006";
  }

  @Override
  public String title() {
    return "Unusually long tool description";
  }

  @Override
  public String owaspRef() {
    return "MCP03";
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
    if (ctx.tool() == null || ctx.snapshot() == null) {
      return List.of();
    }
    int len = ctx.tool().description().length();
    int max = ctx.settings().maxDescriptionLength();
    double median = median(ctx.snapshot().tools());
    boolean absolute = len > max;
    boolean relative =
        ctx.snapshot().tools().size() >= 3
            && len > ctx.settings().relativeLengthFloor()
            && len > ctx.settings().relativeLengthFactor() * median;
    if (!absolute && !relative) {
      return List.of();
    }
    String why =
        absolute
            ? "description is " + len + " chars (limit " + max + ")"
            : "description is "
                + len
                + " chars, over "
                + ctx.settings().relativeLengthFactor()
                + "x the server median of "
                + (int) median;
    return List.of(
        FindingBuilder.of(this, ctx)
            .confidence(0.7)
            .evidence(new Evidence("description", why, 0, len))
            .remediation(
                "Review the full description for hidden instructions; keep descriptions short.")
            .build());
  }

  private static double median(List<ToolDefinition> tools) {
    List<Integer> lens =
        tools.stream()
            .map(t -> t.description().length())
            .sorted(Comparator.naturalOrder())
            .toList();
    if (lens.isEmpty()) {
      return 0;
    }
    int n = lens.size();
    return n % 2 == 1 ? lens.get(n / 2) : (lens.get(n / 2 - 1) + lens.get(n / 2)) / 2.0;
  }
}
