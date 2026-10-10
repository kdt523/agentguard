package io.agentguard.core.check;

import io.agentguard.core.drift.ToolHasher;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolAnnotations;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import io.agentguard.core.model.Transport;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Loads the table-driven check cases from {@code src/test/resources/checks/<CHECK_ID>.yaml}. */
final class CheckCaseLoader {

  private static final YAMLMapper YAML = new YAMLMapper();

  private CheckCaseLoader() {}

  /** One case: the servers to scan, and whether the check under test must report something. */
  record Case(
      String name,
      boolean expectFinding,
      Severity expectSeverity,
      List<ToolSnapshot> snapshots,
      Map<String, McpServerRef> servers) {}

  record CaseFile(String checkId, List<Case> cases) {}

  static List<Path> files() throws IOException {
    Path dir = Path.of("src", "test", "resources", "checks");
    try (var s = Files.list(dir)) {
      return s.filter(p -> p.toString().endsWith(".yaml")).sorted().toList();
    }
  }

  static CaseFile load(Path file) throws IOException {
    JsonNode root = YAML.readTree(Files.readString(file));
    List<Case> cases = new ArrayList<>();
    for (JsonNode c : root.get("cases").values()) {
      cases.add(toCase(c));
    }
    return new CaseFile(root.get("check").asString(), cases);
  }

  private static Case toCase(JsonNode c) {
    List<ToolSnapshot> snaps = new ArrayList<>();
    Map<String, McpServerRef> refs = new LinkedHashMap<>();
    if (c.has("servers")) {
      for (JsonNode s : c.get("servers").values()) {
        addServer(snaps, refs, s);
      }
    } else {
      addServer(snaps, refs, singleServer(c));
    }
    Severity sev =
        c.has("expectSeverity") ? Severity.valueOf(c.get("expectSeverity").asString()) : null;
    return new Case(
        c.get("name").asString(), c.get("expectFinding").booleanValue(), sev, snaps, refs);
  }

  private static JsonNode singleServer(JsonNode c) {
    var node = YAML.createObjectNode();
    node.put("key", "s1");
    var tools = node.putArray("tools");
    if (c.has("tool")) {
      tools.add(c.get("tool"));
    }
    if (c.has("config")) {
      node.set("config", c.get("config"));
    }
    return node;
  }

  private static void addServer(
      List<ToolSnapshot> snaps, Map<String, McpServerRef> refs, JsonNode s) {
    String key = s.get("key").asString();
    List<ToolDefinition> tools = new ArrayList<>();
    if (s.has("tools")) {
      for (JsonNode t : s.get("tools").values()) {
        tools.add(toTool(t));
      }
    }
    snaps.add(
        ToolHasher.hashed(new ToolSnapshot(key, key, "1", Instant.EPOCH, tools, null, null, null)));
    refs.put(key, toRef(key, s.get("config")));
  }

  private static ToolDefinition toTool(JsonNode t) {
    String desc = t.has("description") ? t.get("description").asString() : "";
    if (t.has("descriptionRepeat")) {
      desc = desc.repeat(t.get("descriptionRepeat").asInt());
    }
    JsonNode schema = t.has("schema") ? t.get("schema") : null;
    ToolAnnotations ann = null;
    if (t.has("annotations")) {
      JsonNode a = t.get("annotations");
      ann =
          new ToolAnnotations(
              bool(a, "readOnlyHint"),
              bool(a, "destructiveHint"),
              bool(a, "idempotentHint"),
              bool(a, "openWorldHint"),
              null);
    }
    return new ToolDefinition(t.get("name").asString(), desc, schema, ann, null);
  }

  private static Boolean bool(JsonNode a, String f) {
    return a.has(f) ? a.get(f).booleanValue() : null;
  }

  private static McpServerRef toRef(String key, JsonNode cfg) {
    boolean third = cfg == null || !cfg.has("thirdParty") || cfg.get("thirdParty").booleanValue();
    if (cfg != null && cfg.has("url")) {
      return new McpServerRef(
          key,
          Transport.STREAMABLE_HTTP,
          null,
          null,
          null,
          URI.create(cfg.get("url").asString()),
          third);
    }
    String command = cfg != null && cfg.has("command") ? cfg.get("command").asString() : "python";
    List<String> args = new ArrayList<>();
    if (cfg != null && cfg.has("args")) {
      cfg.get("args").values().forEach(a -> args.add(a.asString()));
    }
    Map<String, String> env = new LinkedHashMap<>();
    if (cfg != null && cfg.has("env")) {
      cfg.get("env").properties().forEach(e -> env.put(e.getKey(), e.getValue().asString()));
    }
    return new McpServerRef(key, Transport.STDIO, command, args, env, null, third);
  }
}
