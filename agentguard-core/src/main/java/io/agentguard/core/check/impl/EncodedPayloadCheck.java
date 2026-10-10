package io.agentguard.core.check.impl;

import io.agentguard.core.check.AbstractTextCheck;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.Match;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.PatternLibrary;
import io.agentguard.core.text.TextAnalysis;
import io.agentguard.core.text.TextField;
import io.agentguard.core.text.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * AG-TP-005: base64 / hex / percent-encoded blobs that decode to readable text. Confidence is high
 * when the decoded text is itself an instruction (TP-001) or a secrecy request (TP-002).
 */
public final class EncodedPayloadCheck extends AbstractTextCheck {

  @Override
  public String id() {
    return "AG-TP-005";
  }

  @Override
  public String title() {
    return "Encoded payload in tool text";
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
    return true;
  }

  @Override
  protected List<Match> detect(CheckContext ctx, TextField field, TextAnalysis analysis) {
    List<Match> out = new ArrayList<>();
    for (TextView v : analysis.views()) {
      if (v.kind() != TextView.Kind.DECODED_ENCODING) {
        continue;
      }
      boolean hostile =
          PatternLibrary.load("AG-TP-001").matches(v.text())
              || PatternLibrary.load("AG-TP-002").matches(v.text());
      String text = "decoded " + v.note() + ": " + v.text();
      out.add(new Match(field.path(), text, 0, text.length(), hostile ? 0.95 : 0.6, true, null));
    }
    return out;
  }

  @Override
  protected String remediation(List<Match> matches) {
    return "Tool text should be plain language. Remove encoded blobs; if the decoded text is an"
        + " instruction, treat the server as malicious.";
  }
}
