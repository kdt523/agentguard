package io.agentguard.core.check.impl;

import io.agentguard.core.check.AbstractTextCheck;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.Match;
import io.agentguard.core.model.Severity;
import io.agentguard.core.text.InvisibleHit;
import io.agentguard.core.text.PatternLibrary;
import io.agentguard.core.text.TextAnalysis;
import io.agentguard.core.text.TextField;
import io.agentguard.core.text.TextView;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AG-TP-004: invisible characters (zero-width, bidi controls, Unicode tag block) in any tool text,
 * and mixed-script confusables in the tool name. CRITICAL when decoded hidden text is itself an
 * instruction or a secrecy request.
 */
public final class InvisibleCharsCheck extends AbstractTextCheck {

  @Override
  public String id() {
    return "AG-TP-004";
  }

  @Override
  public String title() {
    return "Invisible or confusable characters";
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
    if (!analysis.invisibles().isEmpty()) {
      out.add(invisibleMatch(field, analysis));
    }
    if (field.path().equals("name") && mixedScript(field.raw())) {
      String text = "tool name mixes scripts (possible look-alike): " + field.raw();
      out.add(new Match(field.path(), text, 0, text.length(), 0.8, false, null));
    }
    return out;
  }

  private static Match invisibleMatch(TextField field, TextAnalysis a) {
    Map<InvisibleHit.Kind, Integer> counts = new EnumMap<>(InvisibleHit.Kind.class);
    a.invisibles().forEach(h -> counts.merge(h.kind(), 1, Integer::sum));
    String summary =
        counts.entrySet().stream()
            .map(e -> e.getValue() + " " + e.getKey().name().toLowerCase().replace('_', '-'))
            .collect(Collectors.joining(", "));
    String hidden =
        a.views().stream()
            .filter(v -> v.kind() == TextView.Kind.DECODED_TAG)
            .map(TextView::text)
            .collect(Collectors.joining(" "));
    boolean hasTags = counts.containsKey(InvisibleHit.Kind.TAG);
    String text =
        "invisible characters: " + summary + (hidden.isEmpty() ? "" : "; hidden text: " + hidden);
    Severity sev = isInstruction(hidden) ? Severity.CRITICAL : null;
    double conf = hasTags ? 0.95 : 0.7;
    return new Match(field.path(), text, 0, text.length(), conf, hasTags, sev);
  }

  private static boolean isInstruction(String hidden) {
    return !hidden.isEmpty()
        && (PatternLibrary.load("AG-TP-001").matches(hidden)
            || PatternLibrary.load("AG-TP-002").matches(hidden));
  }

  /** True when letters from more than one of Latin, Cyrillic and Greek appear in {@code name}. */
  static boolean mixedScript(String name) {
    List<Character.UnicodeScript> scripts =
        name.codePoints()
            .filter(Character::isLetter)
            .mapToObj(Character.UnicodeScript::of)
            .filter(
                s ->
                    s == Character.UnicodeScript.LATIN
                        || s == Character.UnicodeScript.CYRILLIC
                        || s == Character.UnicodeScript.GREEK)
            .distinct()
            .toList();
    return scripts.size() > 1;
  }

  @Override
  protected String remediation(List<Match> matches) {
    return "Strip invisible characters from tool definitions. Legitimate descriptions never need"
        + " zero-width, bidi or tag characters; inspect the decoded text in the evidence.";
  }
}
