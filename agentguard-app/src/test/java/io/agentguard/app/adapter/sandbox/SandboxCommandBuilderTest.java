package io.agentguard.app.adapter.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentguard.app.config.AgentGuardProperties;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Transport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SandboxCommandBuilderTest {

  private final SandboxCommandBuilder builder =
      new SandboxCommandBuilder(new AgentGuardProperties(null, null, null, null));

  private static McpServerRef npx(Map<String, String> env) {
    return new McpServerRef(
        "GitHub Server", Transport.STDIO, "npx", List.of("-y", "@x/s@1.0.0"), env, null, true);
  }

  @Test
  void run_appliesAllIsolationFlags() {
    var inv = builder.run(npx(Map.of()));

    assertThat(String.join(" ", inv.args()))
        .contains("--network none")
        .contains("--read-only")
        .contains("--tmpfs /tmp:size=64m")
        .contains("--cpus 0.5")
        .contains("--memory 256m")
        .contains("--pids-limit 128")
        .contains("--cap-drop ALL")
        .contains("--security-opt no-new-privileges")
        .contains("--rm -i");
  }

  @Test
  void run_passesOnlyListedEnv() {
    var inv = builder.run(npx(Map.of("TOKEN", "abc")));

    assertThat(inv.args()).contains("TOKEN=abc").noneMatch(a -> a.startsWith("PATH="));
  }

  @Test
  void run_endsWithEntrypointImageAndArgs() {
    var args = builder.run(npx(Map.of())).args();

    assertThat(args.subList(args.size() - 5, args.size()))
        .containsExactly("--entrypoint", "npx", "agentguard/sandbox-node:dev", "-y", "@x/s@1.0.0");
  }

  @Test
  void run_containerNameIsSanitisedAndUnique() {
    var a = builder.run(npx(Map.of()));
    var b = builder.run(npx(Map.of()));

    assertThat(a.containerName())
        .startsWith("agentguard-sbx-github-server-")
        .isNotEqualTo(b.containerName());
  }

  @Test
  void install_usesBridgeNetworkAndSameCacheVolume() {
    var ref = npx(Map.of());
    var inv = builder.install(ref);

    assertThat(String.join(" ", inv.args()))
        .contains("--network bridge")
        .doesNotContain(" -i ")
        .contains(SandboxCommandBuilder.cacheVolume(ref) + ":/cache");
  }

  @Test
  void imageFor_pythonRuntimes_usePythonImage() {
    var uvx = new McpServerRef("u", Transport.STDIO, "/usr/bin/uvx.exe", null, null, null, true);

    assertThat(builder.imageFor(uvx)).isEqualTo("agentguard/sandbox-python:dev");
  }

  @Test
  void sanitize_longKey_isTruncated() {
    assertThat(SandboxCommandBuilder.sanitize("x".repeat(80))).hasSize(40);
  }
}
