package io.agentguard.core.text;

/** An invisible code point found in a text field. */
public record InvisibleHit(int position, int codePoint, Kind kind) {

  /** Category of invisible character. */
  public enum Kind {
    ZERO_WIDTH,
    BIDI_CONTROL,
    TAG
  }

  /** Classifies {@code cp}, or returns null when it is an ordinary visible code point. */
  public static Kind classify(int cp) {
    if ((cp >= 0x200B && cp <= 0x200F) || cp == 0x2060 || cp == 0xFEFF) {
      return Kind.ZERO_WIDTH;
    }
    if ((cp >= 0x202A && cp <= 0x202E) || (cp >= 0x2066 && cp <= 0x2069)) {
      return Kind.BIDI_CONTROL;
    }
    if (cp >= 0xE0000 && cp <= 0xE007F) {
      return Kind.TAG;
    }
    return null;
  }
}
