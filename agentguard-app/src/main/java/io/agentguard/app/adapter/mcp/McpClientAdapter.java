package io.agentguard.app.adapter.mcp;

import io.agentguard.app.adapter.sandbox.SandboxReaper;
import io.agentguard.app.config.AgentGuardProperties;
import io.agentguard.core.drift.ToolHasher;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.PromptDefinition;
import io.agentguard.core.model.ResourceDefinition;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import io.agentguard.core.port.ClockPort;
import io.agentguard.core.port.McpSnapshotPort;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** Fetches tools/prompts/resources from one MCP server with the official Java SDK sync client. */
@Component
public class McpClientAdapter implements McpSnapshotPort {

  private static final int MAX_PAGES = 100;
  private final TransportFactory transports;
  private final SandboxReaper reaper;
  private final ClockPort clock;
  private final AgentGuardProperties props;

  public McpClientAdapter(
      TransportFactory transports,
      SandboxReaper reaper,
      ClockPort clock,
      AgentGuardProperties props) {
    this.transports = transports;
    this.reaper = reaper;
    this.clock = clock;
    this.props = props;
  }

  @Override
  public ToolSnapshot fetch(McpServerRef ref, Duration timeout) {
    TransportFactory.Created created = transports.create(ref);
    McpSyncClient client =
        McpClient.sync(created.transport())
            .clientInfo(new McpSchema.Implementation("agentguard", "0.1.0"))
            .initializationTimeout(props.mcp().connectTimeout())
            .requestTimeout(timeout)
            .build();
    try {
      McpSchema.InitializeResult init = client.initialize();
      List<ToolDefinition> tools =
          paged(c -> client.listTools(c), r -> r.tools(), r -> r.nextCursor()).stream()
              .map(McpSchemaMapper::tool)
              .toList();
      McpSchema.ServerCapabilities caps = init.capabilities();
      List<PromptDefinition> prompts =
          caps != null && caps.prompts() != null
              ? paged(c -> client.listPrompts(c), r -> r.prompts(), r -> r.nextCursor()).stream()
                  .map(McpSchemaMapper::prompt)
                  .toList()
              : List.of();
      List<ResourceDefinition> resources =
          caps != null && caps.resources() != null
              ? paged(c -> client.listResources(c), r -> r.resources(), r -> r.nextCursor())
                  .stream()
                  .map(McpSchemaMapper::resource)
                  .toList()
              : List.of();
      McpSchema.Implementation info = init.serverInfo();
      return ToolHasher.hashed(
          new ToolSnapshot(
              ref.key(),
              info == null ? "" : info.name(),
              info == null ? "" : info.version(),
              clock.now(),
              tools,
              prompts,
              resources,
              null));
    } finally {
      closeQuietly(client);
      created.sandboxContainer().ifPresent(reaper::kill);
    }
  }

  /** Follows {@code nextCursor} pagination, bounded to avoid a malicious endless cursor loop. */
  static <R, T> List<T> paged(
      Function<String, R> call, Function<R, List<T>> items, Function<R, String> next) {
    List<T> out = new ArrayList<>();
    String cursor = null;
    for (int page = 0; page < MAX_PAGES; page++) {
      R r = call.apply(cursor);
      List<T> batch = items.apply(r);
      if (batch != null) {
        out.addAll(batch);
      }
      cursor = next.apply(r);
      if (cursor == null || cursor.isEmpty()) {
        return out;
      }
    }
    throw new IllegalStateException("server returned more than " + MAX_PAGES + " pages");
  }

  private static void closeQuietly(McpSyncClient client) {
    try {
      if (!client.closeGracefully()) {
        client.close();
      }
    } catch (RuntimeException e) {
      client.close();
    }
  }
}
