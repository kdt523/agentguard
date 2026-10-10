package io.agentguard.core.text;

/** One link of the text-detector chain; stages run in order and pass the state along. */
public interface TextStage {
  void apply(TextAnalysis.State state);
}
