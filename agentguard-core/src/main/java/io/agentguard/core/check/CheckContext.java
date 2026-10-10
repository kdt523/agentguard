package io.agentguard.core.check;

import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Input of one check run. {@code snapshot} is null for CROSS_SERVER checks and {@code tool} is null
 * for SERVER and CROSS_SERVER checks. {@code servers} maps server key to its configuration.
 */
public record CheckContext(
    ToolSnapshot snapshot,
    ToolDefinition tool,
    List<ToolSnapshot> allSnapshots,
    CheckSettings settings,
    Map<String, McpServerRef> servers) {

  public CheckContext {
    allSnapshots = allSnapshots == null ? List.of() : List.copyOf(allSnapshots);
    settings = settings == null ? CheckSettings.defaults() : settings;
    servers = servers == null ? Map.of() : Map.copyOf(servers);
  }

  /** Configuration of a server, empty when only the snapshot is known. */
  public Optional<McpServerRef> server(String key) {
    return Optional.ofNullable(servers.get(key));
  }

  /** Unknown servers are treated as third-party (untrusted) to stay conservative. */
  public boolean isThirdParty(String serverKey) {
    return server(serverKey).map(McpServerRef::thirdParty).orElse(true);
  }
}
