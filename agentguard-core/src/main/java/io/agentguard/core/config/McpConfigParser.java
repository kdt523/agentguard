package io.agentguard.core.config;

import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Transport;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses a Claude Desktop / Cursor style {@code {"mcpServers": {...}}} config into server refs. A
 * server is third-party unless its key starts with {@code fixture-} or it is explicitly trusted.
 */
public final class McpConfigParser {

  /** Key prefix of our own fixture servers, which are trusted. */
  public static final String FIXTURE_PREFIX = "fixture-";

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private final Set<String> trustedServers;

  public McpConfigParser(Set<String> trustedServers) {
    this.trustedServers = Set.copyOf(trustedServers);
  }

  /** Parses the JSON text; servers are returned sorted by key. */
  public List<McpServerRef> parse(String json) {
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new ConfigException("config is not valid JSON: " + e.getOriginalMessage());
    }
    return parse(root);
  }

  /** Parses an already-read JSON tree; servers are returned sorted by key. */
  public List<McpServerRef> parse(JsonNode root) {
    JsonNode servers = root == null ? null : root.get("mcpServers");
    if (servers == null || !servers.isObject()) {
      throw new ConfigException("config must contain an object 'mcpServers'");
    }
    List<McpServerRef> refs = new ArrayList<>();
    for (Map.Entry<String, JsonNode> e : servers.properties()) {
      refs.add(toRef(e.getKey(), e.getValue()));
    }
    refs.sort(Comparator.comparing(McpServerRef::key));
    return List.copyOf(refs);
  }

  private McpServerRef toRef(String key, JsonNode node) {
    if (key.isBlank() || !node.isObject()) {
      throw new ConfigException("server '" + key + "' must be a JSON object with a non-blank key");
    }
    boolean thirdParty = !key.startsWith(FIXTURE_PREFIX) && !trustedServers.contains(key);
    String url = text(node, "url");
    String command = text(node, "command");
    if (url != null) {
      return new McpServerRef(
          key, remoteTransport(node, url), null, List.of(), Map.of(), uri(key, url), thirdParty);
    }
    if (command == null) {
      throw new ConfigException("server '" + key + "' needs either 'command' or 'url'");
    }
    return new McpServerRef(
        key,
        Transport.STDIO,
        command,
        strings(key, node.get("args")),
        env(key, node),
        null,
        thirdParty);
  }

  private static Transport remoteTransport(JsonNode node, String url) {
    String type = text(node, "type");
    if ("sse".equalsIgnoreCase(type)
        || (type == null && url.replaceAll("/+$", "").endsWith("/sse"))) {
      return Transport.SSE;
    }
    return Transport.STREAMABLE_HTTP;
  }

  private static URI uri(String key, String url) {
    try {
      URI u = URI.create(url);
      if (u.getScheme() == null || u.getHost() == null) {
        throw new IllegalArgumentException("missing scheme or host");
      }
      return u;
    } catch (IllegalArgumentException e) {
      throw new ConfigException("server '" + key + "' has an invalid url");
    }
  }

  private static List<String> strings(String key, JsonNode args) {
    if (args == null || args.isNull()) {
      return List.of();
    }
    if (!args.isArray()) {
      throw new ConfigException("server '" + key + "': 'args' must be an array");
    }
    List<String> out = new ArrayList<>();
    for (JsonNode a : args.values()) {
      out.add(a.asString());
    }
    return out;
  }

  private static Map<String, String> env(String key, JsonNode node) {
    JsonNode env = node.get("env");
    if (env == null || env.isNull()) {
      return Map.of();
    }
    if (!env.isObject()) {
      throw new ConfigException("server '" + key + "': 'env' must be an object");
    }
    Map<String, String> out = new LinkedHashMap<>();
    for (Map.Entry<String, JsonNode> e : env.properties()) {
      out.put(e.getKey(), e.getValue().asString());
    }
    return out;
  }

  private static String text(JsonNode node, String field) {
    JsonNode v = node.get(field);
    return v == null || v.isNull() || v.asString().isBlank() ? null : v.asString();
  }
}
