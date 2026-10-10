package io.agentguard.core.text;

import java.util.List;

/**
 * Chain of Responsibility over {@link TextStage}s: UnicodeNormaliser, InvisibleCharStripper, then
 * EncodedPayloadExpander. Phrase matchers run afterwards on the resulting views.
 */
public final class TextPipeline {

  /** Fields longer than this are truncated before analysis to bound regex cost. */
  public static final int MAX_CHARS = 50_000;

  private static final TextPipeline STANDARD =
      new TextPipeline(
          List.of(
              new UnicodeNormaliser(), new InvisibleCharStripper(), new EncodedPayloadExpander()));

  private final List<TextStage> stages;

  public TextPipeline(List<TextStage> stages) {
    this.stages = List.copyOf(stages);
  }

  /** The standard three-stage pipeline. */
  public static TextPipeline standard() {
    return STANDARD;
  }

  public TextAnalysis analyze(TextField field) {
    TextAnalysis.State state = new TextAnalysis.State(field);
    if (state.working.length() > MAX_CHARS) {
      state.setWorking(state.working.substring(0, MAX_CHARS));
    }
    for (TextStage s : stages) {
      s.apply(state);
    }
    return state.toAnalysis();
  }
}
