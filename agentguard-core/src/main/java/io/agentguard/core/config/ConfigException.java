package io.agentguard.core.config;

/** Invalid MCP server configuration supplied by the user. Messages never contain env values. */
public class ConfigException extends IllegalArgumentException {

  public ConfigException(String message) {
    super(message);
  }
}
