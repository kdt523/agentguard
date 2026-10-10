package io.agentguard.core.check.impl;

import io.agentguard.core.check.AbstractTextCheck;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.Match;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.PatternLibrary;
import io.agentguard.core.text.TextAnalysis;
import io.agentguard.core.text.TextField;
import java.util.ArrayList;
import java.util.List;

/**
 * AG-SC-001: hidden-instruction or secrecy phrases inside the input schema (description, default,
 * enum, examples, title). Encoded content is reported by AG-TP-005 / AG-TP-004 instead.
 */
public final class InstructionInSchemaCheck extends AbstractTextCheck {

  @Override
  public String id() {
    return "AG-SC-001";
  }

  @Override
  public String title() {
    return "Instruction hidden in input schema";
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
    return field.inSchema();
  }

  @Override
  protected List<Match> detect(CheckContext ctx, TextField field, TextAnalysis analysis) {
    List<Match> out =
        new ArrayList<>(phraseMatches(PatternLibrary.load("AG-TP-001"), field, analysis, false));
    out.addAll(phraseMatches(PatternLibrary.load("AG-TP-002"), field, analysis, false));
    return out;
  }

  @Override
  protected String remediation(List<Match> matches) {
    return "Schema text (descriptions, defaults, enums, examples) is read by the model. Remove the"
        + " instruction and re-pin the tool; treat the server as untrusted.";
  }
}
