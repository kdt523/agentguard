package io.agentguard.core.check;

import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import java.util.List;

/** Strategy: one static check producing zero or more findings for its scope. */
public interface Check {
  /** Stable id such as {@code AG-TP-001}. */
  String id();

  String title();

  /** OWASP MCP Top 10 reference, e.g. {@code MCP03}. */
  String owaspRef();

  Severity defaultSeverity();

  Scope scope();

  List<Finding> run(CheckContext ctx);
}
