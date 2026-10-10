package io.agentguard.app.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed {@code agentguard.*} configuration. */
@ConfigurationProperties("agentguard")
public record AgentGuardProperties(
    Set<String> trustedServers, Mcp mcp, Sandbox sandbox, Remote remote) {

  public AgentGuardProperties {
    trustedServers = trustedServers == null ? Set.of() : Set.copyOf(trustedServers);
    mcp = mcp == null ? new Mcp(null, null) : mcp;
    sandbox = sandbox == null ? Sandbox.defaults() : sandbox;
    remote = remote == null ? new Remote(null) : remote;
  }

  /** MCP client timeouts. */
  public record Mcp(Duration connectTimeout, Duration requestTimeout) {
    public Mcp {
      connectTimeout = connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
      requestTimeout = requestTimeout == null ? Duration.ofSeconds(20) : requestTimeout;
    }
  }

  /** Docker sandbox for third-party stdio servers. */
  public record Sandbox(
      String dockerCommand,
      String nodeImage,
      String pythonImage,
      String cpus,
      String memory,
      int pidsLimit,
      String tmpfsSize,
      Duration installTimeout) {

    public Sandbox {
      dockerCommand = blankTo(dockerCommand, "docker");
      nodeImage = blankTo(nodeImage, "agentguard/sandbox-node:dev");
      pythonImage = blankTo(pythonImage, "agentguard/sandbox-python:dev");
      cpus = blankTo(cpus, "0.5");
      memory = blankTo(memory, "256m");
      pidsLimit = pidsLimit <= 0 ? 128 : pidsLimit;
      tmpfsSize = blankTo(tmpfsSize, "64m");
      installTimeout = installTimeout == null ? Duration.ofMinutes(3) : installTimeout;
    }

    static Sandbox defaults() {
      return new Sandbox(null, null, null, null, null, 0, null, null);
    }
  }

  /** Hosts third-party remote servers may be reached on. Empty means none. */
  public record Remote(List<String> allowedHosts) {
    public Remote {
      allowedHosts = allowedHosts == null ? List.of() : List.copyOf(allowedHosts);
    }
  }

  private static String blankTo(String v, String dflt) {
    return v == null || v.isBlank() ? dflt : v;
  }
}
