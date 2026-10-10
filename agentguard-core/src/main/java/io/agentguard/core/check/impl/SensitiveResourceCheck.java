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
import java.util.Set;

/**
 * AG-TP-003: description points at credential files or asks for secrets. Strong patterns (ssh keys,
 * cloud credentials, dotenv...) always fire; weak "pass the token" phrasing only fires when the
 * tool's own name has nothing to do with secrets (purpose mismatch).
 */
public final class SensitiveResourceCheck extends AbstractTextCheck {

  private static final Set<String> SECRET_PURPOSE =
      Set.of(
          "key",
          "keys",
          "token",
          "tokens",
          "secret",
          "secrets",
          "password",
          "credential",
          "credentials",
          "auth",
          "login",
          "oauth",
          "apikey",
          "vault");

  @Override
  public String id() {
    return "AG-TP-003";
  }

  @Override
  public String title() {
    return "Reference to sensitive resource or credential";
  }

  @Override
  public String owaspRef() {
    return "MCP01";
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
        new ArrayList<>(phraseMatches(PatternLibrary.load("AG-TP-003"), field, analysis, false));
    if (purposeUnrelated(ctx.tool().name())) {
      out.addAll(phraseMatches(PatternLibrary.load("AG-TP-003.weak"), field, analysis, false));
    }
    return out;
  }

  private static boolean purposeUnrelated(String toolName) {
    return ToolText.nameTokens(toolName).stream().noneMatch(SECRET_PURPOSE::contains);
  }

  @Override
  protected String remediation(List<Match> matches) {
    return "A tool should not reference credential files or ask the model to pass secrets. Block"
        + " this tool, rotate any credentials it may have received, and review the server"
        + " source.";
  }
}
