package io.agentguard.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of running the text pipeline on one field: the views of the text and every invisible code
 * point found. Index 0 of {@code views} is always the visible text.
 */
public record TextAnalysis(TextField field, List<TextView> views, List<InvisibleHit> invisibles) {

  public TextAnalysis {
    views = List.copyOf(views);
    invisibles = List.copyOf(invisibles);
  }

  /** The text with invisible characters removed. */
  public String visible() {
    return views.isEmpty() ? "" : views.get(0).text();
  }

  /** Views recovered from hidden or encoded content. */
  public List<TextView> decodedViews() {
    return views.stream().filter(TextView::decoded).toList();
  }

  /** Mutable state passed along the pipeline stages. */
  public static final class State {
    final TextField field;
    String working;
    final List<TextView> views = new ArrayList<>();
    final List<InvisibleHit> invisibles = new ArrayList<>();

    State(TextField field) {
      this.field = field;
      this.working = field.raw();
    }

    /** The text as transformed by the stages so far. */
    public String working() {
      return working;
    }

    public void setWorking(String w) {
      this.working = w;
    }

    public void addView(TextView v) {
      views.add(v);
    }

    public void addInvisible(InvisibleHit h) {
      invisibles.add(h);
    }

    TextAnalysis toAnalysis() {
      return new TextAnalysis(field, views, invisibles);
    }
  }
}
