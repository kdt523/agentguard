package io.agentguard.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** One security issue reported by a check. {@code toolName} is null for server-level findings. */
public record Finding(
    String checkId,
    String owaspRef,
    String title,
    Severity severity,
    int score,
    String serverKey,
    String toolName,
    List<Evidence> evidence,
    String remediation,
    double confidence) {

  public Finding {
    Objects.requireNonNull(checkId, "checkId");
    Objects.requireNonNull(severity, "severity");
    score = Math.max(1, Math.min(100, score));
    confidence = Math.max(0.0, Math.min(1.0, confidence));
    evidence = evidence == null ? List.of() : List.copyOf(evidence);
    owaspRef = owaspRef == null ? "" : owaspRef;
    title = title == null ? "" : title;
    remediation = remediation == null ? "" : remediation;
  }

  /** Tool name, empty for server-level findings. */
  public Optional<String> toolNameOpt() {
    return Optional.ofNullable(toolName);
  }
}
