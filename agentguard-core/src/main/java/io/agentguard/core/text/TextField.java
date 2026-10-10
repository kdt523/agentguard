package io.agentguard.core.text;

/**
 * One piece of attacker-controllable text of a tool, addressed by a path such as {@code
 * inputSchema.properties.sidenote.description}.
 */
public record TextField(String path, String raw) {

  public TextField {
    raw = raw == null ? "" : raw;
  }

  /** True when the text lives inside the input schema (not in the tool's own description). */
  public boolean inSchema() {
    return path.startsWith("inputSchema");
  }

  /** True for the tool's description or annotation title. */
  public boolean isToolText() {
    return path.equals("description") || path.equals("annotations.title");
  }
}
