package io.agentguard.app.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Row of {@code tools}: one tool of one snapshot, queryable by name and hash. */
@Entity
@Table(name = "tools")
public class ToolEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "snapshot_id")
  private SnapshotEntity snapshot;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "input_schema")
  private String inputSchema;

  @JdbcTypeCode(SqlTypes.JSON)
  private String annotations;

  @Column(nullable = false)
  private String sha256;

  protected ToolEntity() {}

  public ToolEntity(
      SnapshotEntity snapshot,
      String name,
      String description,
      String inputSchema,
      String annotations,
      String sha256) {
    this.id = UUID.randomUUID();
    this.snapshot = snapshot;
    this.name = name;
    this.description = description;
    this.inputSchema = inputSchema;
    this.annotations = annotations;
    this.sha256 = sha256;
  }

  public String getName() {
    return name;
  }

  public String getSha256() {
    return sha256;
  }
}
