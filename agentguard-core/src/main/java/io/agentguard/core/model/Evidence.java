package io.agentguard.core.model;

/** Where a finding was observed; {@code excerpt} is redacted and at most 200 chars. */
public record Evidence(String field, String excerpt, int start, int end) {

  /** Maximum excerpt length. */
  public static final int MAX_EXCERPT = 200;

  public Evidence {
    field = field == null ? "" : field;
    excerpt = excerpt == null ? "" : excerpt;
    if (excerpt.length() > MAX_EXCERPT) {
      excerpt = excerpt.substring(0, MAX_EXCERPT);
    }
  }
}
