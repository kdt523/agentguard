package io.agentguard.core.check;

import io.agentguard.core.model.Severity;

/**
 * A detector hit inside {@code text}[start,end) of one field. {@code severity} overrides the
 * check's default when non-null; {@code decoded} marks hits found in hidden/encoded content.
 */
public record Match(
    String field,
    String text,
    int start,
    int end,
    double confidence,
    boolean decoded,
    Severity severity) {

  /** A visible-text match with the check's default severity. */
  public static Match visible(String field, String text, int start, int end, double confidence) {
    return new Match(field, text, start, end, confidence, false, null);
  }
}
