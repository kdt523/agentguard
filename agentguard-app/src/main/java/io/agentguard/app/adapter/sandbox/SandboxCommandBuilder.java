package io.agentguard.app.adapter.sandbox;

import io.agentguard.app.config.AgentGuardProperties;
import io.agentguard.core.model.McpServerRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Builds the {@code docker run} command lines that confine third-party stdio servers: no network,
 * read-only root, dropped capabilities, CPU/memory/pid limits, and only the env the user listed.
 */
@Component
public class SandboxCommandBuilder {

  /** Label put on every sandbox container (used for cleanup and tests). */
  public static final String LABEL = "agentguard.sandbox=true";

  private static final String CACHE_MOUNT = "/cache";
  private final AgentGuardProperties.Sandbox cfg;

  public SandboxCommandBuilder(AgentGuardProperties props) {
    this.cfg = props.sandbox();
  }

  /** A docker invocation: the executable, its arguments, and the container name it creates. */
  public record DockerInvocation(String command, List<String> args, String containerName) {}

  /** The locked-down run of the server, speaking MCP over stdin/stdout. */
  public DockerInvocation run(McpServerRef ref) {
    String name = containerName(ref);
    List<String> a = new ArrayList<>(List.of("run", "--rm", "-i", "--name", name));
    a.addAll(
        List.of(
            "--label",
            LABEL,
            "--network",
            "none",
            "--read-only",
            "--tmpfs",
            "/tmp:size=" + cfg.tmpfsSize(),
            "--cpus",
            cfg.cpus(),
            "--memory",
            cfg.memory(),
            "--pids-limit",
            String.valueOf(cfg.pidsLimit()),
            "--cap-drop",
            "ALL",
            "--security-opt",
            "no-new-privileges",
            "-e",
            "HOME=/tmp",
            "-e",
            "npm_config_offline=true",
            "-e",
            "UV_OFFLINE=1"));
    commonCacheArgs(ref, a);
    appendServer(ref, a);
    return new DockerInvocation(cfg.dockerCommand(), List.copyOf(a), name);
  }

  /**
   * One-time install with network access into the server's cache volume. The server is started with
   * stdin closed, so it downloads its packages and exits.
   */
  public DockerInvocation install(McpServerRef ref) {
    String name = containerName(ref) + "-install";
    List<String> a = new ArrayList<>(List.of("run", "--rm", "--name", name, "--label", LABEL));
    a.addAll(
        List.of(
            "--network", "bridge",
            "--cpus", cfg.cpus(),
            "--memory", "1g",
            "--cap-drop", "ALL",
            "--security-opt", "no-new-privileges",
            "-e", "HOME=/tmp"));
    commonCacheArgs(ref, a);
    appendServer(ref, a);
    return new DockerInvocation(cfg.dockerCommand(), List.copyOf(a), name);
  }

  /** The image chosen for this server's runtime (node for npx/node, python otherwise). */
  public String imageFor(McpServerRef ref) {
    String cmd = baseName(ref.command());
    return switch (cmd) {
      case "npx", "node", "npm", "pnpm", "bunx" -> cfg.nodeImage();
      default -> cfg.pythonImage();
    };
  }

  /** Name of the per-server package cache volume. */
  public static String cacheVolume(McpServerRef ref) {
    return "agentguard-sbx-cache-" + sanitize(ref.key());
  }

  private void commonCacheArgs(McpServerRef ref, List<String> a) {
    a.addAll(List.of("-v", cacheVolume(ref) + ":" + CACHE_MOUNT));
    a.addAll(List.of("-e", "npm_config_cache=" + CACHE_MOUNT + "/npm"));
    a.addAll(List.of("-e", "UV_CACHE_DIR=" + CACHE_MOUNT + "/uv"));
    a.addAll(List.of("-e", "UV_TOOL_DIR=" + CACHE_MOUNT + "/uv-tools"));
    // Only env the user listed; values go via -e KEY=VALUE, never inherited from the host.
    for (Map.Entry<String, String> e : ref.env().entrySet()) {
      a.addAll(List.of("-e", e.getKey() + "=" + e.getValue()));
    }
  }

  private void appendServer(McpServerRef ref, List<String> a) {
    a.addAll(List.of("--entrypoint", ref.command(), imageFor(ref)));
    a.addAll(ref.args());
  }

  private static String containerName(McpServerRef ref) {
    return "agentguard-sbx-"
        + sanitize(ref.key())
        + "-"
        + UUID.randomUUID().toString().substring(0, 8);
  }

  static String sanitize(String key) {
    String s = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "-");
    return s.length() > 40 ? s.substring(0, 40) : s;
  }

  private static String baseName(String command) {
    String c = command.replace('\\', '/');
    c = c.substring(c.lastIndexOf('/') + 1);
    return c.toLowerCase(Locale.ROOT).replaceAll("\\.(exe|cmd)$", "");
  }
}
