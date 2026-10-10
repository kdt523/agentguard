package io.agentguard.app.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Row of {@code servers}. {@code config} never holds env values, only env keys. */
@Entity
@Table(name = "servers")
public class ServerEntity {

  @Id private UUID id;

  @Column(name = "key", nullable = false, unique = true)
  private String key;

  @Column(nullable = false)
  private String transport;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private String config;

  @Column(name = "third_party", nullable = false)
  private boolean thirdParty;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ServerEntity() {}

  public ServerEntity(
      String key, String transport, String config, boolean thirdParty, Instant now) {
    this.id = UUID.randomUUID();
    this.key = key;
    this.transport = transport;
    this.config = config;
    this.thirdParty = thirdParty;
    this.createdAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public void update(String transport, String config, boolean thirdParty) {
    this.transport = transport;
    this.config = config;
    this.thirdParty = thirdParty;
  }
}
