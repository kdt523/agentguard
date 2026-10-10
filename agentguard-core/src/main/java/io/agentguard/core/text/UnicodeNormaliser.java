package io.agentguard.core.text;

import java.text.Normalizer;

/** Stage 1: NFKC so look-alike forms (full-width letters, ligatures) match plain patterns. */
public final class UnicodeNormaliser implements TextStage {

  @Override
  public void apply(TextAnalysis.State state) {
    state.setWorking(Normalizer.normalize(state.working(), Normalizer.Form.NFKC));
  }
}
