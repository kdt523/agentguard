package io.agentguard.core.model;

import java.util.Map;

/** Overall risk score 0..1000, its band and every contributing term. */
public record ScoreBreakdown(int total, String band, Map<String, Integer> components) {

  public ScoreBreakdown {
    components = components == null ? Map.of() : Map.copyOf(components);
  }
}
