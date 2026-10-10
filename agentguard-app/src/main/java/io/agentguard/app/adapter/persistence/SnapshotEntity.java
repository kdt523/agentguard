package io.agentguard.app.adapter.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Row of {@code snapshots}; {@code raw} is the full serialised {@code ToolSnapshot}. */
@Entity
@Table(name = "snapshots")
public class SnapshotEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "server_id")
  private ServerEntity server;

  @Column(name = "fetched_at", nullable = false)
  private Instant fetchedAt;

  @Column(name = "server_name")
  private String serverName;

  @Column(name = "server_version")
  private String serverVersion;

  @Column(name = "snapshot_sha256", nullable = false)
  private String snapshotSha256;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private String raw;

  @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ToolEntity> tools = new ArrayList<>();

  protected SnapshotEntity() {}

  public SnapshotEntity(
      ServerEntity server,
      Instant fetchedAt,
      String serverName,
      String serverVersion,
      String snapshotSha256,
      String raw) {
    this.id = UUID.randomUUID();
    this.server = server;
    this.fetchedAt = fetchedAt;
    this.serverName = serverName;
    this.serverVersion = serverVersion;
    this.snapshotSha256 = snapshotSha256;
    this.raw = raw;
  }

  public void addTool(ToolEntity tool) {
    tools.add(tool);
  }

  public UUID getId() {
    return id;
  }

  public Instant getFetchedAt() {
    return fetchedAt;
  }

  public void setFetchedAt(Instant fetchedAt) {
    this.fetchedAt = fetchedAt;
  }

  public String getSnapshotSha256() {
    return snapshotSha256;
  }

  public String getRaw() {
    return raw;
  }

  public List<ToolEntity> getTools() {
    return tools;
  }
}
