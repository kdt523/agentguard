package io.agentguard.core.model;

import java.util.List;

/** Smallest set of tools whose removal breaks the given fraction of toxic paths. */
public record CutRecommendation(List<String> toolIds, double pathsRemovedFraction) {

  public CutRecommendation {
    toolIds = toolIds == null ? List.of() : List.copyOf(toolIds);
  }

  /** A recommendation that removes nothing (no toxic paths). */
  public static CutRecommendation none() {
    return new CutRecommendation(List.of(), 0.0);
  }
}
