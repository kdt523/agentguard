package io.agentguard.core.check.impl;

import io.agentguard.core.check.AbstractTextCheck;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.Match;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.PatternLibrary;
import io.agentguard.core.text.TextAnalysis;
import io.agentguard.core.text.TextField;
import java.util.List;

/** AG-TP-002: text telling the model to hide its actions from the user. */
public final class SecrecyCheck extends AbstractTextCheck {

  @Override
  public String id() {
    return "AG-TP-002";
  }

  @Override
  public String title() {
    return "Secrecy or user-deception instruction";
  }

  @Override
  public String owaspRef() {
    return "MCP03";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.HIGH;
  }

  @Override
  protected boolean appliesTo(TextField field) {
    return field.isToolText();
  }

  @Override
  protected List<Match> detect(CheckContext ctx, TextField field, TextAnalysis analysis) {
    return phraseMatches(PatternLibrary.load("AG-TP-002"), field, analysis, false);
  }

  @Override
  protected String remediation(List<Match> matches) {
    return "A legitimate tool never needs to hide behaviour from the user. Treat the server as"
        + " untrusted and remove it until the description is corrected.";
  }
}
