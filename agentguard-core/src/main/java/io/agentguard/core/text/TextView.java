package io.agentguard.core.text;

/**
 * One reading of a text field: the visible text, or text recovered from an invisible/encoded
 * payload. Phrase checks treat decoded views differently from visible ones.
 */
public record TextView(String text, Kind kind, String note) {

  /** How this view was obtained. */
  public enum Kind {
    VISIBLE,
    DECODED_TAG,
    DECODED_ENCODING
  }

  /** True when the text was hidden in the original field. */
  public boolean decoded() {
    return kind != Kind.VISIBLE;
  }
}
