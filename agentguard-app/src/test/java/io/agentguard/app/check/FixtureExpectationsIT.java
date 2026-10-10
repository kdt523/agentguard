package io.agentguard.app.check;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentguard.app.PostgresContainerSupport;
import io.agentguard.app.fixtures.FixtureCatalog;
import io.agentguard.app.service.SnapshotService;
import io.agentguard.core.check.CheckEngine;
import io.agentguard.core.model.CaptureResult;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.ToolSnapshot;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;

/**
 * The phase-3 gate: scanning the real fixture servers must reproduce {@code fixtures/expected.yaml}
 * exactly (no misses, no false positives on clean-*), both per server and all together.
 */
@Tag("fixtures")
@SpringBootTest
@Import(PostgresContainerSupport.class)
class FixtureExpectationsIT {

  @Autowired SnapshotService snapshots;
  @Autowired CheckEngine engine;

  @Test
  void scan_fixtures_reproducesExpectedYamlExactly() {
    List<McpServerRef> refs =
        FixtureCatalog.toolsByFixture().keySet().stream().map(FixtureCatalog::stdio).toList();
    CaptureResult capture = snapshots.fetchAll(refs);
    assertThat(capture.errors()).isEmpty();
    Map<String, McpServerRef> byKey =
        refs.stream().collect(Collectors.toMap(McpServerRef::key, Function.identity()));

    Map<String, TreeSet<String>> expectedPerServer = expectedPerTool();
    Map<String, TreeSet<String>> actualPerServer = new TreeMap<>();
    for (ToolSnapshot s : capture.snapshots()) {
      actualPerServer.putAll(group(engine.scan(List.of(s), byKey)));
      initEmpty(actualPerServer, s);
    }
    assertThat(actualPerServer).as("per-server scans").isEqualTo(expectedPerServer);

    Map<String, TreeSet<String>> expectedCombined = expectedPerTool();
    crossServerExpectations().forEach((k, v) -> expectedCombined.get(k).addAll(v));
    Map<String, TreeSet<String>> actualCombined = group(engine.scan(capture.snapshots(), byKey));
    capture.snapshots().forEach(s -> initEmpty(actualCombined, s));
    assertThat(actualCombined).as("all fixtures scanned together").isEqualTo(expectedCombined);
  }

  /** "fixture/tool" to the set of expected check ids, with an entry for every tool. */
  private static Map<String, TreeSet<String>> expectedPerTool() {
    Map<String, TreeSet<String>> out = new TreeMap<>();
    for (Map.Entry<String, JsonNode> server :
        FixtureCatalog.expected().get("servers").properties()) {
      for (Map.Entry<String, JsonNode> tool : server.getValue().get("tools").properties()) {
        TreeSet<String> ids = new TreeSet<>();
        tool.getValue().get("expect").values().forEach(v -> ids.add(v.asString()));
        out.put(server.getKey() + "/" + tool.getKey(), ids);
      }
    }
    return out;
  }

  private static Map<String, List<String>> crossServerExpectations() {
    Map<String, List<String>> out = new TreeMap<>();
    for (Map.Entry<String, JsonNode> e :
        FixtureCatalog.expected().get("cross_server").properties()) {
      out.put(e.getKey(), e.getValue().values().stream().map(JsonNode::asString).toList());
    }
    return out;
  }

  /** Groups findings by "fixture/tool"; server-level findings use the key "fixture/(server)". */
  private static Map<String, TreeSet<String>> group(List<Finding> findings) {
    Map<String, TreeSet<String>> out = new TreeMap<>();
    for (Finding f : findings) {
      String fixture = f.serverKey().substring("fixture-".length());
      String tool = f.toolName() == null ? "(server)" : f.toolName();
      out.computeIfAbsent(fixture + "/" + tool, k -> new TreeSet<>()).add(f.checkId());
    }
    return out;
  }

  private static void initEmpty(Map<String, TreeSet<String>> map, ToolSnapshot s) {
    String fixture = s.serverKey().substring("fixture-".length());
    s.tools().forEach(t -> map.computeIfAbsent(fixture + "/" + t.name(), k -> new TreeSet<>()));
  }
}
