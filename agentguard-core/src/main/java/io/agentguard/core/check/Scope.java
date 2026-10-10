package io.agentguard.core.check;

/** At which level a check runs. */
public enum Scope {
  /** Once per tool. */
  TOOL,
  /** Once per server snapshot (and its configuration). */
  SERVER,
  /** Once per scan, seeing all servers (e.g. name collisions). */
  CROSS_SERVER
}
