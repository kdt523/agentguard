package io.agentguard.core.text;

import io.agentguard.core.model.ToolDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Collects every text field of a tool that a model could read: description, title, schema text. */
public final class TextFieldExtractor {

  private static final Set<String> TEXT_KEYS =
      Set.of("description", "title", "default", "const", "enum", "examples");
  private static final int MAX_DEPTH = 24;

  private TextFieldExtractor() {}

  /** All text fields of {@code tool}, in a stable order. */
  public static List<TextField> fields(ToolDefinition tool) {
    List<TextField> out = new ArrayList<>();
    out.add(new TextField("name", tool.name()));
    if (!tool.description().isEmpty()) {
      out.add(new TextField("description", tool.description()));
    }
    tool.annotationsOpt()
        .map(a -> a.title())
        .filter(t -> !t.isEmpty())
        .ifPresent(t -> out.add(new TextField("annotations.title", t)));
    walk(tool.inputSchema(), "inputSchema", out, 0);
    return out;
  }

  private static void walk(JsonNode node, String path, List<TextField> out, int depth) {
    if (depth > MAX_DEPTH || node == null) {
      return;
    }
    if (node.isObject()) {
      for (Map.Entry<String, JsonNode> e : node.properties()) {
        String child = path + "." + e.getKey();
        if (TEXT_KEYS.contains(e.getKey())) {
          collect(e.getValue(), child, out);
        }
        walk(e.getValue(), child, out, depth + 1);
      }
    } else if (node.isArray()) {
      int i = 0;
      for (JsonNode v : node.values()) {
        walk(v, path + "[" + i++ + "]", out, depth + 1);
      }
    }
  }

  private static void collect(JsonNode value, String path, List<TextField> out) {
    if (value.isString()) {
      if (!value.asString().isEmpty()) {
        out.add(new TextField(path, value.asString()));
      }
    } else if (value.isArray()) {
      int i = 0;
      for (JsonNode v : value.values()) {
        if (v.isString() && !v.asString().isEmpty()) {
          out.add(new TextField(path + "[" + i + "]", v.asString()));
        }
        i++;
      }
    }
  }
}
