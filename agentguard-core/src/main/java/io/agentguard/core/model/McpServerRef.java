package io.agentguard.core.model;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reference to an MCP server to scan. {@code env} values are secrets and are never printed: {@link
 * #toString()} only shows env keys.
 */
public record McpServerRef(
    String key,
    Transport transport,
    String command,
    List<String> args,
    Map<String, String> env,
    URI url,
    boolean thirdParty) {

  public McpServerRef {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(transport, "transport");
    args = args == null ? List.of() : List.copyOf(args);
    env = env == null ? Map.of() : Map.copyOf(env);
    if (transport == Transport.STDIO && (command == null || command.isBlank())) {
      throw new IllegalArgumentException("stdio server '" + key + "' requires a command");
    }
    if (transport != Transport.STDIO && url == null) {
      throw new IllegalArgumentException("remote server '" + key + "' requires a url");
    }
  }

  /** Stdio command, empty for remote servers. */
  public Optional<String> commandOpt() {
    return Optional.ofNullable(command);
  }

  /** Remote URL, empty for stdio servers. */
  public Optional<URI> urlOpt() {
    return Optional.ofNullable(url);
  }

  @Override
  public String toString() {
    return "McpServerRef[key=%s, transport=%s, command=%s, args=%s, envKeys=%s, url=%s, thirdParty=%s]"
        .formatted(key, transport, command, args, env.keySet(), url, thirdParty);
  }
}
