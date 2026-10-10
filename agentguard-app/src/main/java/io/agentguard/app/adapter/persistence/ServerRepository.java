package io.agentguard.app.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ServerEntity}. */
public interface ServerRepository extends JpaRepository<ServerEntity, UUID> {
  Optional<ServerEntity> findByKey(String key);
}
