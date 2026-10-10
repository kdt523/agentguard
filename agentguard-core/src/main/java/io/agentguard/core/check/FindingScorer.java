package io.agentguard.core.check;

import io.agentguard.core.model.Severity;

/**
 * Per-finding score: severity base, +5 third-party, +5 decoded payload, -15 when confidence &lt;
 * 0.5.
 */
public final class FindingScorer {

  private FindingScorer() {}

  public static int score(
      Severity severity, boolean thirdParty, boolean decoded, double confidence) {
    int v = severity.base();
    if (thirdParty) {
      v += 5;
    }
    if (decoded) {
      v += 5;
    }
    if (confidence < 0.5) {
      v -= 15;
    }
    return Math.max(1, Math.min(100, v));
  }
}
