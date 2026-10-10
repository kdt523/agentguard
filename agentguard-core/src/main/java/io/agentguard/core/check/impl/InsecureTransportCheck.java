package io.agentguard.core.check.impl;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.FindingBuilder;
import io.agentguard.core.check.Scope;
import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Severity;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** AG-CF-003: a remote MCP server reached over plain http:// outside localhost. */
public final class InsecureTransportCheck implements Check {

  private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

  @Override
  public String id() {
    return "AG-CF-003";
  }

  @Override
  public String title() {
    return "Insecure remote transport (http)";
  }

  @Override
  public String owaspRef() {
    return "MCP07";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.MEDIUM;
  }

  @Override
  public Scope scope() {
    return Scope.SERVER;
  }

  @Override
  public List<Finding> run(CheckContext ctx) {
    Optional<McpServerRef> ref = ctx.server(ctx.snapshot().serverKey());
    if (ref.isEmpty() || ref.get().url() == null) {
      return List.of();
    }
    var url = ref.get().url();
    boolean plain = "http".equalsIgnoreCase(url.getScheme());
    if (!plain || LOCAL_HOSTS.contains(String.valueOf(url.getHost()).toLowerCase())) {
      return List.of();
    }
    String text = "remote server reached over unencrypted http://" + url.getHost();
    return List.of(
        FindingBuilder.of(this, ctx)
            .confidence(0.95)
            .evidence(new Evidence("url", text, 0, text.length()))
            .remediation(
                "Use https:// so tool definitions and calls cannot be read or modified in transit.")
            .build());
  }
}
