package io.agentguard.core.text;

/**
 * Stage 2: records every invisible code point, removes them to form the VISIBLE view, and decodes
 * the Unicode tag block (U+E0000..U+E007F, an ASCII-in-disguise channel) into its own view.
 */
public final class InvisibleCharStripper implements TextStage {

  @Override
  public void apply(TextAnalysis.State state) {
    String text = state.working();
    StringBuilder visible = new StringBuilder(text.length());
    StringBuilder tags = new StringBuilder();
    int[] cps = text.codePoints().toArray();
    for (int i = 0; i < cps.length; i++) {
      int cp = cps[i];
      InvisibleHit.Kind kind = InvisibleHit.classify(cp);
      if (kind == null || isLegitimate(cps, i, kind)) {
        visible.appendCodePoint(cp);
        continue;
      }
      state.addInvisible(new InvisibleHit(i, cp, kind));
      if (kind == InvisibleHit.Kind.TAG) {
        int ascii = cp - 0xE0000;
        if (ascii >= 0x20 && ascii < 0x7F) {
          tags.append((char) ascii);
        }
      }
    }
    state.addView(new TextView(visible.toString(), TextView.Kind.VISIBLE, null));
    if (!tags.isEmpty()) {
      state.addView(new TextView(tags.toString(), TextView.Kind.DECODED_TAG, "unicode tag block"));
    }
    state.setWorking(visible.toString());
  }

  /** Zero-width joiners inside emoji sequences, and LRM/RLM in RTL text, are normal. */
  private static boolean isLegitimate(int[] cps, int i, InvisibleHit.Kind kind) {
    int cp = cps[i];
    if (kind == InvisibleHit.Kind.ZERO_WIDTH && cp == 0x200D) {
      return i > 0 && i + 1 < cps.length && isEmoji(cps[i - 1]) && isEmoji(cps[i + 1]);
    }
    if (kind == InvisibleHit.Kind.ZERO_WIDTH && (cp == 0x200E || cp == 0x200F)) {
      for (int c : cps) {
        if (isRtl(c)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isEmoji(int cp) {
    return cp >= 0x1F000 || (cp >= 0x2600 && cp <= 0x27BF) || cp == 0xFE0F;
  }

  private static boolean isRtl(int cp) {
    return (cp >= 0x0590 && cp <= 0x08FF) || (cp >= 0xFB1D && cp <= 0xFEFC);
  }
}
