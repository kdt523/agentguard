package io.agentguard.app.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link SnapshotEntity}. */
public interface SnapshotRepository extends JpaRepository<SnapshotEntity, UUID> {
  Optional<SnapshotEntity> findFirstByServerKeyOrderByFetchedAtDesc(String serverKey);

  long countByServerKey(String serverKey);
}
