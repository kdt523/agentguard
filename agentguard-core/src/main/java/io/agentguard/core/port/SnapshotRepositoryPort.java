package io.agentguard.core.port;

import io.agentguard.core.model.ToolSnapshot;
import java.util.Optional;

/** Persists tool snapshots. */
public interface SnapshotRepositoryPort {
  ToolSnapshot save(ToolSnapshot s);

  Optional<ToolSnapshot> latest(String serverKey);
}
