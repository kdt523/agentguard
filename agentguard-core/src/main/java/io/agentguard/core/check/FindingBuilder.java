package io.agentguard.core.check;

import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.Redactor;
import java.util.ArrayList;
import java.util.List;

/** Builder for findings: fills check metadata, scores, and redacts evidence excerpts. */
public final class FindingBuilder {

  private final Check check;
  private final CheckContext ctx;
  private Severity severity;
  private String title;
  private String remediation = "";
  private double confidence = 1.0;
  private boolean decoded;
  private String serverKey;
  private String toolName;
  private final List<Evidence> evidence = new ArrayList<>();

  private FindingBuilder(Check check, CheckContext ctx) {
    this.check = check;
    this.ctx = ctx;
    this.severity = check.defaultSeverity();
    this.title = check.title();
    this.serverKey = ctx.snapshot() == null ? null : ctx.snapshot().serverKey();
    this.toolName = ctx.tool() == null ? null : ctx.tool().name();
  }

  public static FindingBuilder of(Check check, CheckContext ctx) {
    return new FindingBuilder(check, ctx);
  }

  public FindingBuilder severity(Severity s) {
    this.severity = s;
    return this;
  }

  public FindingBuilder title(String t) {
    this.title = t;
    return this;
  }

  public FindingBuilder remediation(String r) {
    this.remediation = r;
    return this;
  }

  public FindingBuilder confidence(double c) {
    this.confidence = c;
    return this;
  }

  public FindingBuilder decoded(boolean d) {
    this.decoded = d;
    return this;
  }

  /** Overrides the server/tool the finding is attached to (cross-server checks). */
  public FindingBuilder target(String serverKey, String toolName) {
    this.serverKey = serverKey;
    this.toolName = toolName;
    return this;
  }

  public FindingBuilder evidence(Evidence e) {
    this.evidence.add(e);
    return this;
  }

  /** Adds evidence for text[start,end) with up to 200 chars of redacted context. */
  public FindingBuilder evidence(String field, String text, int start, int end) {
    return evidence(excerpt(field, text, start, end));
  }

  public Finding build() {
    int score = FindingScorer.score(severity, ctx.isThirdParty(serverKey), decoded, confidence);
    return new Finding(
        check.id(),
        check.owaspRef(),
        title,
        severity,
        score,
        serverKey,
        toolName,
        evidence,
        remediation,
        confidence);
  }

  /** Window of at most {@link Evidence#MAX_EXCERPT} chars around [start,end), redacted. */
  public static Evidence excerpt(String field, String text, int start, int end) {
    int s = Math.max(0, Math.min(start, text.length()));
    int e = Math.max(s, Math.min(end, text.length()));
    int budget = Evidence.MAX_EXCERPT - 2;
    int matchLen = Math.min(e - s, budget);
    int pad = (budget - matchLen) / 2;
    int from = Math.max(0, s - pad);
    int to = Math.min(text.length(), from + budget);
    String window = text.substring(from, to).replaceAll("\\s+", " ");
    return new Evidence(field, Redactor.redact(window).strip(), s, e);
  }
}
