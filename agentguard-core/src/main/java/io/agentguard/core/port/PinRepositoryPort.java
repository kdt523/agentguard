package io.agentguard.core.port;

import java.util.Map;

/** Stores approved tool hashes (pins) used for rug-pull detection. */
public interface PinRepositoryPort {
  void pin(String serverKey, String toolName, String sha256, String approvedBy);

  /** Tool name to sha256 for the server; empty map when nothing is pinned. */
  Map<String, String> pinsFor(String serverKey);
}
