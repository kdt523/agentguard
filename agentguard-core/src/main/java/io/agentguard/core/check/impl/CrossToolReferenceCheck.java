package io.agentguard.core.check.impl;

import io.agentguard.core.check.AbstractTextCheck;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.Match;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import io.agentguard.core.text.PatternLibrary;
import io.agentguard.core.text.TextAnalysis;
import io.agentguard.core.text.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AG-SH-001: a description that talks about another tool, either by the exact identifier of a tool
 * on a different server or through "when the X tool is used" style phrasing. Plain-word tool names
 * (e.g. "add") are ignored on purpose to avoid false positives.
 */
public final class CrossToolReferenceCheck extends AbstractTextCheck {

  @Override
  public String id() {
    return "AG-SH-001";
  }

  @Override
  public String title() {
    return "Description references another tool (shadowing)";
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
    List<Match> out =
        new ArrayList<>(phraseMatches(PatternLibrary.load("AG-SH-001"), field, analysis, false));
    String text = analysis.visible();
    for (ToolSnapshot other : ctx.allSnapshots()) {
      if (other.serverKey().equals(ctx.snapshot().serverKey())) {
        continue;
      }
      for (ToolDefinition t : other.tools()) {
        if (ToolText.isIdentifierLike(t.name())) {
          addMention(out, field, text, t.name());
        }
      }
    }
    return out;
  }

  private static void addMention(List<Match> out, TextField field, String text, String toolName) {
    Matcher m =
        Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(toolName) + "(?![A-Za-z0-9_])")
            .matcher(text);
    if (m.find()) {
      out.add(Match.visible(field.path(), text, m.start(), m.end(), 0.9));
    }
  }

  @Override
  protected String remediation(List<Match> matches) {
    return "A tool description must not refer to or alter other tools. Remove the cross-tool text"
        + " or disable this server; check which server owns the referenced tool.";
  }
}
