package io.agentguard.app.fixtures;

import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Transport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Reads {@code fixtures/expected.yaml} and builds refs to the phase-1 fixture images. */
public final class FixtureCatalog {

  private static final JsonNode EXPECTED = load();

  private FixtureCatalog() {}

  public static JsonNode expected() {
    return EXPECTED;
  }

  /** Fixture name to its sorted tool names, as recorded in expected.yaml. */
  public static Map<String, List<String>> toolsByFixture() {
    Map<String, List<String>> out = new TreeMap<>();
    for (Map.Entry<String, JsonNode> e : EXPECTED.get("servers").properties()) {
      out.put(e.getKey(), List.copyOf(new TreeSet<>(e.getValue().get("tools").propertyNames())));
    }
    return out;
  }

  public static String image(String fixture) {
    return "agentguard/fixture-" + fixture + ":dev";
  }

  /** Trusted stdio ref that runs the fixture image with no network. */
  public static McpServerRef stdio(String fixture) {
    return new McpServerRef(
        "fixture-" + fixture,
        Transport.STDIO,
        "docker",
        List.of("run", "--rm", "-i", "--network", "none", image(fixture)),
        Map.of(),
        null,
        false);
  }

  private static JsonNode load() {
    Path p = Path.of("..", "fixtures", "expected.yaml");
    if (!Files.exists(p)) {
      p = Path.of("fixtures", "expected.yaml");
    }
    try {
      return new YAMLMapper().readTree(Files.readString(p));
    } catch (java.io.IOException e) {
      throw new IllegalStateException("cannot read " + p.toAbsolutePath(), e);
    }
  }
}
