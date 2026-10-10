package io.agentguard.core.check.impl;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.FindingBuilder;
import io.agentguard.core.check.Scope;
import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * AG-SH-003: the same tool name offered by two or more servers, so one can shadow the other.
 * Severity is raised to HIGH when a third-party server collides with a trusted one.
 */
public final class ToolNameCollisionCheck implements Check {

  @Override
  public String id() {
    return "AG-SH-003";
  }

  @Override
  public String title() {
    return "Tool name collision across servers";
  }

  @Override
  public String owaspRef() {
    return "MCP03";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.MEDIUM;
  }

  @Override
  public Scope scope() {
    return Scope.CROSS_SERVER;
  }

  @Override
  public List<Finding> run(CheckContext ctx) {
    Map<String, TreeSet<String>> serversByTool = new TreeMap<>();
    for (ToolSnapshot s : ctx.allSnapshots()) {
      for (ToolDefinition t : s.tools()) {
        serversByTool.computeIfAbsent(t.name(), k -> new TreeSet<>()).add(s.serverKey());
      }
    }
    List<Finding> out = new ArrayList<>();
    serversByTool.forEach(
        (tool, servers) -> {
          if (servers.size() > 1) {
            servers.forEach(server -> out.add(finding(ctx, tool, server, servers)));
          }
        });
    return out;
  }

  private Finding finding(CheckContext ctx, String tool, String server, TreeSet<String> servers) {
    boolean mixedTrust =
        servers.stream().anyMatch(ctx::isThirdParty)
            && servers.stream().anyMatch(s -> !ctx.isThirdParty(s));
    String text = "tool '" + tool + "' is also provided by: " + String.join(", ", servers);
    return FindingBuilder.of(this, ctx)
        .target(server, tool)
        .severity(mixedTrust ? Severity.HIGH : Severity.MEDIUM)
        .confidence(0.8)
        .evidence(new Evidence("name", text, 0, text.length()))
        .remediation(
            "Namespace tools per server in your client, or disable one of the servers, so a"
                + " malicious server cannot shadow a trusted tool of the same name.")
        .build();
  }
}
