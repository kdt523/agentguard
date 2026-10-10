package io.agentguard.app.adapter.persistence;

import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import io.agentguard.core.port.ClockPort;
import io.agentguard.core.port.SnapshotRepositoryPort;
import java.util.Optional;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Persists snapshots. Dedup: if the latest snapshot of a server has the same {@code
 * snapshotSha256}, no new row is written; only {@code fetched_at} moves forward.
 */
@Component
public class JpaSnapshotRepositoryAdapter implements SnapshotRepositoryPort {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final ServerRepository servers;
  private final SnapshotRepository snapshots;
  private final ClockPort clock;

  public JpaSnapshotRepositoryAdapter(
      ServerRepository servers, SnapshotRepository snapshots, ClockPort clock) {
    this.servers = servers;
    this.snapshots = snapshots;
    this.clock = clock;
  }

  /** Records (or updates) the server row; stores env keys only, never values. */
  @Transactional
  public void registerServer(McpServerRef ref) {
    String config = serverConfig(ref);
    servers
        .findByKey(ref.key())
        .ifPresentOrElse(
            s -> s.update(ref.transport().name(), config, ref.thirdParty()),
            () ->
                servers.save(
                    new ServerEntity(
                        ref.key(), ref.transport().name(), config, ref.thirdParty(), clock.now())));
  }

  @Override
  @Transactional
  public ToolSnapshot save(ToolSnapshot s) {
    Optional<SnapshotEntity> latest =
        snapshots.findFirstByServerKeyOrderByFetchedAtDesc(s.serverKey());
    if (latest.isPresent() && latest.get().getSnapshotSha256().equals(s.snapshotSha256())) {
      latest.get().setFetchedAt(s.fetchedAt());
      return s;
    }
    ServerEntity server =
        servers
            .findByKey(s.serverKey())
            .orElseGet(
                () ->
                    servers.save(
                        new ServerEntity(s.serverKey(), "UNKNOWN", "{}", true, clock.now())));
    SnapshotEntity e =
        new SnapshotEntity(
            server,
            s.fetchedAt(),
            s.serverName(),
            s.serverVersion(),
            s.snapshotSha256(),
            JSON.writeValueAsString(s));
    for (ToolDefinition t : s.tools()) {
      e.addTool(
          new ToolEntity(
              e,
              t.name(),
              t.description(),
              JSON.writeValueAsString(t.inputSchema()),
              t.annotations() == null ? null : JSON.writeValueAsString(t.annotations()),
              t.sha256()));
    }
    snapshots.save(e);
    return s;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ToolSnapshot> latest(String serverKey) {
    return snapshots
        .findFirstByServerKeyOrderByFetchedAtDesc(serverKey)
        .map(
            e -> {
              ToolSnapshot raw = JSON.readValue(e.getRaw(), ToolSnapshot.class);
              return new ToolSnapshot(
                  raw.serverKey(),
                  raw.serverName(),
                  raw.serverVersion(),
                  e.getFetchedAt(),
                  raw.tools(),
                  raw.prompts(),
                  raw.resources(),
                  raw.snapshotSha256());
            });
  }

  /** Number of stored snapshot rows for a server (used to verify dedup). */
  @Transactional(readOnly = true)
  public long countSnapshots(String serverKey) {
    return snapshots.countByServerKey(serverKey);
  }

  static String serverConfig(McpServerRef ref) {
    ObjectNode n = JSON.createObjectNode();
    ref.commandOpt().ifPresent(c -> n.put("command", c));
    n.putPOJO("args", ref.args());
    n.putPOJO("envKeys", new TreeSet<>(ref.env().keySet()));
    ref.urlOpt().ifPresent(u -> n.put("url", u.toString()));
    return JSON.writeValueAsString(n);
  }
}
