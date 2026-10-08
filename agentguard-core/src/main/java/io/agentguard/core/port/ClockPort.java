package io.agentguard.core.port;

import java.time.Instant;

/** Source of time; all time in AgentGuard flows through this port. */
public interface ClockPort {
  Instant now();
}
