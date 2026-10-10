package io.agentguard.core.check;

import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.PatternLibrary;
import io.agentguard.core.text.TextAnalysis;
import io.agentguard.core.text.TextField;
import io.agentguard.core.text.TextFieldExtractor;
import io.agentguard.core.text.TextPipeline;
import io.agentguard.core.text.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Template Method for checks that look at a tool's text: extracts fields, runs the text pipeline,
 * delegates detection to the subclass and folds all hits of a tool into one finding.
 */
public abstract class AbstractTextCheck implements Check {

  @Override
  public final Scope scope() {
    return Scope.TOOL;
  }

  @Override
  public final List<Finding> run(CheckContext ctx) {
    if (ctx.tool() == null) {
      return List.of();
    }
    List<Match> matches = new ArrayList<>();
    for (TextField f : TextFieldExtractor.fields(ctx.tool())) {
      if (appliesTo(f)) {
        matches.addAll(detect(ctx, f, TextPipeline.standard().analyze(f)));
      }
    }
    return matches.isEmpty() ? List.of() : List.of(toFinding(ctx, matches));
  }

  /** Which text fields this check inspects. */
  protected abstract boolean appliesTo(TextField field);

  /** Hits of this check in one analysed field. */
  protected abstract List<Match> detect(CheckContext ctx, TextField field, TextAnalysis analysis);

  /** Remediation text for the given hits. */
  protected abstract String remediation(List<Match> matches);

  private Finding toFinding(CheckContext ctx, List<Match> matches) {
    FindingBuilder b = FindingBuilder.of(this, ctx);
    Severity sev = defaultSeverity();
    double best = 0;
    boolean decoded = false;
    for (Match m : matches) {
      b.evidence(m.field(), m.text(), m.start(), m.end());
      best = Math.max(best, m.confidence());
      decoded |= m.decoded();
      if (m.severity() != null && m.severity().base() > sev.base()) {
        sev = m.severity();
      }
    }
    return b.severity(sev)
        .confidence(best)
        .decoded(decoded)
        .remediation(remediation(matches))
        .build();
  }

  /** Runs {@code lib} over the visible view (and decoded views when {@code includeDecoded}). */
  protected static List<Match> phraseMatches(
      PatternLibrary lib, TextField field, TextAnalysis analysis, boolean includeDecoded) {
    List<Match> out = new ArrayList<>();
    for (TextView v : analysis.views()) {
      if (v.decoded() && !includeDecoded) {
        continue;
      }
      for (PatternLibrary.Hit h : lib.find(v.text())) {
        out.add(
            new Match(
                field.path(),
                v.text(),
                h.start(),
                h.end(),
                h.rule().confidence(),
                v.decoded(),
                null));
      }
    }
    return out;
  }
}
