package io.agentguard.core.text;

import java.util.List;
import java.util.regex.Pattern;

/** Masks anything that looks like a credential before it reaches logs, evidence or reports. */
public final class Redactor {

  /** Replacement for redacted text. */
  public static final String MASK = "[REDACTED]";

  private static final List<Pattern> SECRETS =
      List.of(
          Pattern.compile("sk-[A-Za-z0-9_-]{16,}"),
          Pattern.compile("AKIA[0-9A-Z]{16}"),
          Pattern.compile("gh[pousr]_[A-Za-z0-9]{20,}"),
          Pattern.compile("xox[abprs]-[A-Za-z0-9-]{10,}"),
          Pattern.compile("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}"),
          Pattern.compile("(?<![0-9A-Fa-f])[0-9A-Fa-f]{40,}(?![0-9A-Fa-f])"),
          Pattern.compile("(?<![A-Za-z0-9+/])[A-Za-z0-9+/]{40,}={0,2}"));

  private Redactor() {}

  /** {@code text} with every secret-looking token replaced by {@link #MASK}. */
  public static String redact(String text) {
    String out = text;
    for (Pattern p : SECRETS) {
      out = p.matcher(out).replaceAll(MASK);
    }
    return out;
  }

  /** True when the whole value (or a token in it) looks like a credential. */
  public static boolean looksLikeSecret(String value) {
    return SECRETS.stream().anyMatch(p -> p.matcher(value).find());
  }
}
