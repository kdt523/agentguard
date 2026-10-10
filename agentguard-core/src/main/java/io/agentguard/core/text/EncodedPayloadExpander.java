package io.agentguard.core.text;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stage 3: finds base64 / hex / percent-encoded blobs and adds a DECODED_ENCODING view for each
 * blob that decodes to mostly-printable text.
 */
public final class EncodedPayloadExpander implements TextStage {

  /** Minimum length of an encoded blob worth decoding. */
  static final int MIN_BLOB = 24;

  private static final Pattern BASE64 =
      Pattern.compile(
          "(?<![A-Za-z0-9+/=_-])[A-Za-z0-9+/]{" + MIN_BLOB + ",}={0,2}(?![A-Za-z0-9+/])");
  private static final Pattern HEX =
      Pattern.compile("(?<![0-9A-Fa-f])(?:[0-9A-Fa-f]{2}){12,}(?![0-9A-Fa-f])");
  private static final Pattern PERCENT = Pattern.compile("(?:%[0-9A-Fa-f]{2}){6,}");

  @Override
  public void apply(TextAnalysis.State state) {
    String text = state.working();
    expand(state, BASE64, text, "base64");
    expand(state, HEX, text, "hex");
    expand(state, PERCENT, text, "percent-encoding");
  }

  private static void expand(TextAnalysis.State state, Pattern p, String text, String kind) {
    Matcher m = p.matcher(text);
    while (m.find()) {
      String decoded = decode(kind, m.group());
      if (decoded != null && looksLikeText(decoded)) {
        state.addView(new TextView(decoded, TextView.Kind.DECODED_ENCODING, kind));
      }
    }
  }

  private static String decode(String kind, String blob) {
    try {
      byte[] bytes =
          switch (kind) {
            case "base64" -> Base64.getDecoder().decode(pad(blob));
            case "hex" -> HexFormat.of().parseHex(blob);
            default -> percentBytes(blob);
          };
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (IllegalArgumentException | CharacterCodingException e) {
      return null;
    }
  }

  private static String pad(String b64) {
    int rem = b64.length() % 4;
    return rem == 0 ? b64 : b64 + "=".repeat(4 - rem);
  }

  private static byte[] percentBytes(String s) {
    byte[] out = new byte[s.length() / 3];
    for (int i = 0; i < out.length; i++) {
      out[i] = (byte) Integer.parseInt(s.substring(i * 3 + 1, i * 3 + 3), 16);
    }
    return out;
  }

  /** At least 8 chars, ≥90% printable, and mostly letters/spaces (rejects random binary). */
  static boolean looksLikeText(String s) {
    if (s.length() < 8) {
      return false;
    }
    long printable = s.chars().filter(c -> c >= 0x20 && c < 0x7F || c == '\n' || c == '\t').count();
    long wordy = s.chars().filter(c -> Character.isLetter(c) || c == ' ').count();
    return printable >= s.length() * 0.9 && wordy >= s.length() * 0.6;
  }
}
