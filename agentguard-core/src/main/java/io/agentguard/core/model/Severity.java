package io.agentguard.core.model;

/** Finding severity with the base score used by scoring. */
public enum Severity {
  CRITICAL(95),
  HIGH(80),
  MEDIUM(55),
  LOW(25),
  INFO(5);

  private final int base;

  Severity(int base) {
    this.base = base;
  }

  /** Base score 1..100 for this severity. */
  public int base() {
    return base;
  }

  /** True when this severity is at least as severe as {@code other}. */
  public boolean atLeast(Severity other) {
    return base >= other.base;
  }
}
