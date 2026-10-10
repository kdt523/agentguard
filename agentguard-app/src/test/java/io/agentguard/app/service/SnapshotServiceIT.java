package io.agentguard.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentguard.app.PostgresContainerSupport;
import io.agentguard.app.adapter.persistence.JpaSnapshotRepositoryAdapter;
import io.agentguard.app.fixtures.FixtureCatalog;
import io.agentguard.core.model.CaptureResult;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import io.agentguard.core.model.Transport;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@Tag("fixtures")
@SpringBootTest
@Import(PostgresContainerSupport.class)
class SnapshotServiceIT {

  @Autowired SnapshotService service;
  @Autowired JpaSnapshotRepositoryAdapter repository;

  private static Map<String, List<String>> toolNames(
      CaptureResult r, Function<String, String> key) {
    return r.snapshots().stream()
        .collect(
            Collectors.toMap(
                s -> key.apply(s.serverKey()),
                s -> s.tools().stream().map(ToolDefinition::name).sorted().toList()));
  }

  @Test
  void capture_everyFixtureOverStdio_matchesExpectedToolsAndPersists() {
    List<McpServerRef> refs =
        FixtureCatalog.toolsByFixture().keySet().stream().map(FixtureCatalog::stdio).toList();

    CaptureResult r = service.capture(refs);

    assertThat(r.errors()).isEmpty();
    assertThat(toolNames(r, k -> k.substring("fixture-".length())))
        .isEqualTo(FixtureCatalog.toolsByFixture());
    assertThat(repository.latest("fixture-poisoned-math"))
        .hasValueSatisfying(s -> assertThat(s.snapshotSha256()).hasSize(64));
  }

  @Test
  void capture_everyFixtureOverStreamableHttp_matchesExpectedTools() {
    List<GenericContainer<?>> containers = new ArrayList<>();
    List<McpServerRef> refs = new ArrayList<>();
    try {
      for (String f : FixtureCatalog.toolsByFixture().keySet()) {
        GenericContainer<?> c =
            new GenericContainer<>(FixtureCatalog.image(f))
                .withCommand("--transport", "http")
                .withExposedPorts(8000)
                .waitingFor(Wait.forListeningPort());
        c.start();
        containers.add(c);
        URI url = URI.create("http://" + c.getHost() + ":" + c.getMappedPort(8000) + "/mcp");
        refs.add(
            new McpServerRef(
                "fixture-" + f + "-http", Transport.STREAMABLE_HTTP, null, null, null, url, false));
      }

      CaptureResult r = service.fetchAll(refs);

      assertThat(r.errors()).isEmpty();
      assertThat(toolNames(r, k -> k.substring(8, k.length() - 5)))
          .isEqualTo(FixtureCatalog.toolsByFixture());
    } finally {
      containers.forEach(GenericContainer::stop);
    }
  }

  @Test
  void capture_twice_hashStableAndDeduplicated() {
    McpServerRef ref = FixtureCatalog.stdio("schema-tricks");

    ToolSnapshot first = service.capture(List.of(ref)).snapshots().get(0);
    ToolSnapshot second = service.capture(List.of(ref)).snapshots().get(0);

    assertThat(second.snapshotSha256()).isEqualTo(first.snapshotSha256());
    assertThat(repository.countSnapshots(ref.key())).isEqualTo(1);
  }

  @Test
  void capture_brokenServer_yieldsServerErrorNotFailure() {
    McpServerRef broken =
        new McpServerRef(
            "fixture-broken",
            Transport.STDIO,
            "docker",
            List.of("run", "--rm", "-i", "agentguard/does-not-exist:dev"),
            null,
            null,
            false);

    CaptureResult r = service.capture(List.of(broken, FixtureCatalog.stdio("clean-weather")));

    assertThat(r.snapshots()).hasSize(1);
    assertThat(r.errors())
        .singleElement()
        .satisfies(e -> assertThat(e.serverKey()).isEqualTo("fixture-broken"));
  }
}
