package io.agentguard.core.check;

import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import io.agentguard.core.text.Redactor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs TOOL checks per tool, SERVER checks per snapshot and CROSS_SERVER checks once; removes
 * duplicate findings and sorts by score (desc). A check that throws becomes an INFO finding {@code
 * AG-ENG-000} instead of aborting the scan.
 */
public final class CheckEngine {

  /** Id used when a check itself fails. */
  public static final String ENGINE_FAILURE_ID = "AG-ENG-000";

  private final CheckRegistry registry;
  private final CheckSettings settings;

  public CheckEngine(CheckRegistry registry, CheckSettings settings) {
    this.registry = registry;
    this.settings = settings;
  }

  public CheckEngine(CheckRegistry registry) {
    this(registry, CheckSettings.defaults());
  }

  public List<Finding> scan(List<ToolSnapshot> snapshots, Map<String, McpServerRef> servers) {
    List<Finding> all = new ArrayList<>();
    for (ToolSnapshot s : snapshots) {
      for (Check c : registry.withScope(Scope.SERVER)) {
        all.addAll(run(c, new CheckContext(s, null, snapshots, settings, servers)));
      }
      for (ToolDefinition t : s.tools()) {
        for (Check c : registry.withScope(Scope.TOOL)) {
          all.addAll(run(c, new CheckContext(s, t, snapshots, settings, servers)));
        }
      }
    }
    for (Check c : registry.withScope(Scope.CROSS_SERVER)) {
      all.addAll(run(c, new CheckContext(null, null, snapshots, settings, servers)));
    }
    return sort(dedup(all));
  }

  private List<Finding> run(Check check, CheckContext ctx) {
    try {
      return check.run(ctx);
    } catch (RuntimeException e) {
      String serverKey = ctx.snapshot() == null ? null : ctx.snapshot().serverKey();
      String tool = ctx.tool() == null ? null : ctx.tool().name();
      return List.of(
          new Finding(
              ENGINE_FAILURE_ID,
              "",
              "Check " + check.id() + " failed to run",
              Severity.INFO,
              Severity.INFO.base(),
              serverKey,
              tool,
              List.of(
                  new Evidence(
                      "engine",
                      Redactor.redact(e.getClass().getSimpleName() + ": " + e.getMessage()),
                      0,
                      0)),
              "Report this as a bug; the rest of the scan is unaffected.",
              1.0));
    }
  }

  private static List<Finding> dedup(List<Finding> findings) {
    Map<String, Finding> unique = new LinkedHashMap<>();
    for (Finding f : findings) {
      String spans =
          f.evidence().stream()
              .map(e -> e.field() + ":" + e.start() + "-" + e.end())
              .sorted()
              .toList()
              .toString();
      unique.putIfAbsent(f.checkId() + "|" + f.serverKey() + "|" + f.toolName() + "|" + spans, f);
    }
    return new ArrayList<>(unique.values());
  }

  private static List<Finding> sort(List<Finding> findings) {
    return findings.stream()
        .sorted(
            Comparator.comparingInt(Finding::score)
                .reversed()
                .thenComparing(Finding::checkId)
                .thenComparing(f -> String.valueOf(f.serverKey()))
                .thenComparing(f -> String.valueOf(f.toolName())))
        .toList();
  }
}
