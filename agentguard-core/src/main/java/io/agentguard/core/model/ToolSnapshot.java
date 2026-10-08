package io.agentguard.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Everything one MCP server advertised at one point in time. */
public record ToolSnapshot(
    String serverKey,
    String serverName,
    String serverVersion,
    Instant fetchedAt,
    List<ToolDefinition> tools,
    List<PromptDefinition> prompts,
    List<ResourceDefinition> resources,
    String snapshotSha256) {

  public ToolSnapshot {
    Objects.requireNonNull(serverKey, "serverKey");
    serverName = serverName == null ? "" : serverName;
    serverVersion = serverVersion == null ? "" : serverVersion;
    tools = tools == null ? List.of() : List.copyOf(tools);
    prompts = prompts == null ? List.of() : List.copyOf(prompts);
    resources = resources == null ? List.of() : List.copyOf(resources);
    snapshotSha256 = snapshotSha256 == null ? "" : snapshotSha256;
  }
}
