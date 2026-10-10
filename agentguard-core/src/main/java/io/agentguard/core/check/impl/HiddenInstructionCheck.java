package io.agentguard.core.check.impl;

import io.agentguard.core.check.AbstractTextCheck;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.Match;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.PatternLibrary;
import io.agentguard.core.text.TextAnalysis;
import io.agentguard.core.text.TextField;
import java.util.List;

/** AG-TP-001: instructions to the model hidden in a tool description. */
public final class HiddenInstructionCheck extends AbstractTextCheck {

  @Override
  public String id() {
    return "AG-TP-001";
  }

  @Override
  public String title() {
    return "Hidden instruction block in tool description";
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
    return phraseMatches(PatternLibrary.load("AG-TP-001"), field, analysis, false);
  }

  @Override
  protected String remediation(List<Match> matches) {
    return "Do not use this server. Tool descriptions must describe the tool, not instruct the"
        + " model. Remove the hidden instruction block or report it to the server's"
        + " maintainer.";
  }
}
