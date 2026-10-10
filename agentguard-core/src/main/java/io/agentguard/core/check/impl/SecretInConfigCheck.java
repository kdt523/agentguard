package io.agentguard.core.check.impl;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.FindingBuilder;
import io.agentguard.core.check.Scope;
import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.Redactor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * AG-CF-002: env values in the MCP config that look like real credentials. The value is never put
 * into evidence, only the variable name.
 */
public final class SecretInConfigCheck implements Check {

  private static final Pattern SECRET_NAME =
      Pattern.compile(
          "(token|secret|password|passwd|api[_-]?key|private[_-]?key)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PLACEHOLDER =
      Pattern.compile(
          "^(\\$|<|\\{\\{|%|your[_-]|xxx|changeme|example|placeholder)", Pattern.CASE_INSENSITIVE);

  @Override
  public String id() {
    return "AG-CF-002";
  }

  @Override
  public String title() {
    return "Secret stored in MCP configuration";
  }

  @Override
  public String owaspRef() {
    return "MCP01";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.HIGH;
  }

  @Override
  public Scope scope() {
    return Scope.SERVER;
  }

  @Override
  public List<Finding> run(CheckContext ctx) {
    Optional<McpServerRef> ref = ctx.server(ctx.snapshot().serverKey());
    if (ref.isEmpty()) {
      return List.of();
    }
    FindingBuilder b = FindingBuilder.of(this, ctx);
    boolean known = false;
    boolean guessed = false;
    for (Map.Entry<String, String> e : new TreeMap<>(ref.get().env()).entrySet()) {
      boolean pattern = Redactor.looksLikeSecret(e.getValue());
      boolean byName = !pattern && looksLikeNamedSecret(e.getKey(), e.getValue());
      if (pattern || byName) {
        String text = "environment variable " + e.getKey() + " holds a credential-like value";
        b.evidence(new Evidence("env." + e.getKey(), text, 0, text.length()));
        known |= pattern;
        guessed |= byName;
      }
    }
    if (!known && !guessed) {
      return List.of();
    }
    return List.of(
        b.confidence(known ? 0.9 : 0.6)
            .remediation(
                "Do not commit credentials in MCP config files. Reference them from the environment"
                    + " or a secret manager, and rotate any value that was committed.")
            .build());
  }

  private static boolean looksLikeNamedSecret(String key, String value) {
    return SECRET_NAME.matcher(key).find()
        && value.length() >= 12
        && !PLACEHOLDER.matcher(value).find();
  }
}
