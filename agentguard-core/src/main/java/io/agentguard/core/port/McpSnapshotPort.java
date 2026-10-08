package io.agentguard.core.port;

import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.ToolSnapshot;
import java.time.Duration;

/** Fetches the current tool snapshot of an MCP server. */
public interface McpSnapshotPort {
  ToolSnapshot fetch(McpServerRef ref, Duration timeout);
}
