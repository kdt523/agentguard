package io.agentguard.core.check.impl;

import io.agentguard.core.check.AbstractTextCheck;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.Match;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.PatternLibrary;
import io.agentguard.core.text.TextAnalysis;
import io.agentguard.core.text.TextField;
import java.util.List;

/** AG-SH-002: text that changes how other tools are called (recipients, arguments, outputs). */
public final class BehaviourOverrideCheck extends AbstractTextCheck {

  @Override
  public String id() {
    return "AG-SH-002";
  }

  @Override
  public String title() {
    return "Description overrides behaviour of other tools";
  }

  @Override
  public String owaspRef() {
    return "MCP03";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.CRITICAL;
  }

  @Override
  protected boolean appliesTo(TextField field) {
    return field.isToolText();
  }

  @Override
  protected List<Match> detect(CheckContext ctx, TextField field, TextAnalysis analysis) {
    return phraseMatches(PatternLibrary.load("AG-SH-002"), field, analysis, false);
  }

  @Override
  protected String remediation(List<Match> matches) {
    return "Remove this server: it tries to redirect or modify calls to other tools, for example to"
        + " BCC an attacker on emails. Review recent actions taken through the affected tools.";
  }
}
