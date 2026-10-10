package io.agentguard.core.drift;

import io.agentguard.core.model.ToolAnnotations;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/** Canonical sha256 of tools and snapshots, the basis of pinning and rug-pull detection. */
public final class ToolHasher {

  private static final JsonNodeFactory F = JsonNodeFactory.instance;

  private ToolHasher() {}

  /** sha256 over canonical {name, description, inputSchema, annotations}. */
  public static String sha256(ToolDefinition tool) {
    ObjectNode n = F.objectNode();
    n.put("name", tool.name());
    n.put("description", tool.description());
    n.set("inputSchema", tool.inputSchema());
    n.set("annotations", annotations(tool.annotations()));
    return sha256(CanonicalJson.write(n));
  }

  /** Copy of {@code tool} with its {@code sha256} field filled in. */
  public static ToolDefinition withHash(ToolDefinition tool) {
    return new ToolDefinition(
        tool.name(), tool.description(), tool.inputSchema(), tool.annotations(), sha256(tool));
  }

  /** sha256 of the sorted tool hashes joined by newline. */
  public static String snapshotSha256(List<ToolDefinition> tools) {
    List<String> hashes = tools.stream().map(ToolHasher::sha256).sorted().toList();
    return sha256(String.join("\n", hashes));
  }

  /** Copy of {@code s} whose tools carry hashes and whose snapshot hash is computed. */
  public static ToolSnapshot hashed(ToolSnapshot s) {
    List<ToolDefinition> tools = s.tools().stream().map(ToolHasher::withHash).toList();
    return new ToolSnapshot(
        s.serverKey(),
        s.serverName(),
        s.serverVersion(),
        s.fetchedAt(),
        tools,
        s.prompts(),
        s.resources(),
        snapshotSha256(tools));
  }

  private static ObjectNode annotations(ToolAnnotations a) {
    if (a == null) {
      return null;
    }
    ObjectNode n = F.objectNode();
    putIfSet(n, "readOnlyHint", a.readOnlyHint());
    putIfSet(n, "destructiveHint", a.destructiveHint());
    putIfSet(n, "idempotentHint", a.idempotentHint());
    putIfSet(n, "openWorldHint", a.openWorldHint());
    if (a.title() != null) {
      n.put("title", a.title());
    }
    return n;
  }

  private static void putIfSet(ObjectNode n, String key, Boolean value) {
    if (value != null) {
      n.put(key, value);
    }
  }

  static String sha256(String text) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
