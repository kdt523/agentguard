package io.agentguard.core.drift;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Deterministic JSON serialisation for hashing: object keys sorted, no whitespace, numbers
 * normalised (1.0 == 1), strings NFC-normalised. Invisible characters are kept, so hiding text
 * changes the hash.
 */
public final class CanonicalJson {

  private CanonicalJson() {}

  /** Canonical string form of {@code node}; {@code null} serialises as {@code null}. */
  public static String write(JsonNode node) {
    StringBuilder sb = new StringBuilder();
    append(sb, node);
    return sb.toString();
  }

  private static void append(StringBuilder sb, JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      sb.append("null");
    } else if (node.isObject()) {
      appendObject(sb, node);
    } else if (node.isArray()) {
      sb.append('[');
      boolean first = true;
      for (JsonNode v : node.values()) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        append(sb, v);
      }
      sb.append(']');
    } else if (node.isNumber()) {
      sb.append(number(node.decimalValue()));
    } else if (node.isBoolean()) {
      sb.append(node.booleanValue());
    } else {
      appendString(sb, node.asString());
    }
  }

  private static void appendObject(StringBuilder sb, JsonNode node) {
    List<Map.Entry<String, JsonNode>> entries = new ArrayList<>(node.properties());
    entries.sort(Map.Entry.comparingByKey());
    sb.append('{');
    for (int i = 0; i < entries.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      appendString(sb, entries.get(i).getKey());
      sb.append(':');
      append(sb, entries.get(i).getValue());
    }
    sb.append('}');
  }

  static String number(BigDecimal d) {
    BigDecimal s = d.stripTrailingZeros();
    return s.signum() == 0 ? "0" : s.toPlainString();
  }

  /** Appends a JSON string literal; non-ASCII is escaped as \\uXXXX so output is pure ASCII. */
  static void appendString(StringBuilder sb, String raw) {
    String s = Normalizer.normalize(raw, Normalizer.Form.NFC);
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20 || c > 0x7e) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    sb.append('"');
  }
}
