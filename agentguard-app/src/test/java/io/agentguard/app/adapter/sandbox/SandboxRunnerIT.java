package io.agentguard.app.adapter.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentguard.app.adapter.mcp.TransportFactory;
import io.agentguard.app.config.AgentGuardProperties;
import io.agentguard.app.fixtures.FixtureCatalog;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Transport;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Runs a fixture as if it were third-party and checks the sandbox really isolates it. */
@Tag("fixtures")
class SandboxRunnerIT {

  private final AgentGuardProperties props =
      new AgentGuardProperties(
          null,
          null,
          new AgentGuardProperties.Sandbox(
              null, null, FixtureCatalog.image("clean-calculator"), null, null, 0, null, null),
          null);
  private final TransportFactory factory =
      new TransportFactory(new SandboxCommandBuilder(props), props);
  private final SandboxReaper reaper = new SandboxReaper(props);

  @Test
  void thirdPartyStdio_runsWithNoNetwork_andIsRemovedAfter() throws Exception {
    McpServerRef ref =
        new McpServerRef(
            "probe", Transport.STDIO, "python", List.of("server.py"), Map.of("A", "1"), null, true);
    TransportFactory.Created created = factory.create(ref);
    String name = created.sandboxContainer().orElseThrow();
    McpSyncClient client =
        McpClient.sync(created.transport()).requestTimeout(Duration.ofSeconds(30)).build();
    String networkMode;
    String readOnly;
    try {
      client.initialize();
      assertThat(client.listTools().tools()).hasSize(2);
      networkMode = docker("inspect", "-f", "{{.HostConfig.NetworkMode}}", name);
      readOnly = docker("inspect", "-f", "{{.HostConfig.ReadonlyRootfs}}", name);
    } finally {
      client.closeGracefully();
      reaper.kill(name);
    }

    assertThat(networkMode).isEqualTo("none");
    assertThat(readOnly).isEqualTo("true");
    assertThat(docker("ps", "-a", "-q", "--filter", "name=" + name)).isEmpty();
  }

  private static String docker(String... args) throws IOException, InterruptedException {
    List<String> cmd = new java.util.ArrayList<>(List.of("docker"));
    cmd.addAll(List.of(args));
    Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
    String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    p.waitFor();
    return out;
  }
}
