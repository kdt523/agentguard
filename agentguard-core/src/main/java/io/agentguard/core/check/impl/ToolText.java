package io.agentguard.core.check.impl;

import io.agentguard.core.model.ToolDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/** Small helpers shared by checks: name tokenising, verb detection, schema parameter access. */
final class ToolText {

  private static final Pattern WORD = Pattern.compile("[A-Za-z]+");
  private static final int FIRST_SENTENCE_MAX = 200;

  private ToolText() {}

  /** Lower-case tokens of a tool or parameter name (snake, kebab and camel case). */
  static List<String> nameTokens(String name) {
    String spaced = name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    List<String> out = new ArrayList<>();
    for (String t : spaced.split("[^a-z0-9]+")) {
      if (!t.isEmpty()) {
        out.add(t);
      }
    }
    return out;
  }

  /** The first sentence of {@code description}, at most 200 chars. */
  static String firstSentence(String description) {
    String d = description.strip();
    int end = d.length();
    Matcher m = Pattern.compile("[.!?](\\s|$)|\\n").matcher(d);
    if (m.find()) {
      end = m.start();
    }
    return d.substring(0, Math.min(end, FIRST_SENTENCE_MAX));
  }

  /** Lower-case words of {@code text}. */
  static List<String> words(String text) {
    List<String> out = new ArrayList<>();
    Matcher m = WORD.matcher(text);
    while (m.find()) {
      out.add(m.group().toLowerCase(Locale.ROOT));
    }
    return out;
  }

  /** The first verb from {@code verbs} found in the tool name or its first description sentence. */
  static String findVerb(ToolDefinition tool, Set<String> verbs) {
    List<String> candidates = new ArrayList<>(nameTokens(tool.name()));
    candidates.addAll(words(firstSentence(tool.description())));
    for (String w : candidates) {
      if (isVerb(w, verbs)) {
        return w;
      }
    }
    return null;
  }

  private static boolean isVerb(String w, Set<String> verbs) {
    return verbs.contains(w)
        || (w.endsWith("s") && verbs.contains(w.substring(0, w.length() - 1)))
        || (w.endsWith("es") && verbs.contains(w.substring(0, w.length() - 2)));
  }

  /** Named input parameters of the tool, in schema order. */
  static Map<String, JsonNode> params(ToolDefinition tool) {
    Map<String, JsonNode> out = new LinkedHashMap<>();
    JsonNode props = tool.inputSchema().get("properties");
    if (props != null && props.isObject()) {
      props.properties().forEach(e -> out.put(e.getKey(), e.getValue()));
    }
    return out;
  }

  /**
   * True for identifier-like tool names (snake, kebab or camel case) that are unlikely to be plain
   * words.
   */
  static boolean isIdentifierLike(String name) {
    return name.length() >= 4
        && (name.contains("_") || name.contains("-") || name.matches(".*[a-z][A-Z].*"));
  }
}
