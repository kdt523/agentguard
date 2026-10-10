package io.agentguard.core.check;

/** Tunable thresholds for checks. */
public record CheckSettings(
    int maxDescriptionLength, double relativeLengthFactor, int relativeLengthFloor) {

  /** Defaults from the phase-3 spec: 1,500 chars, 3x the server median, never below 300 chars. */
  public static CheckSettings defaults() {
    return new CheckSettings(1500, 3.0, 300);
  }
}
