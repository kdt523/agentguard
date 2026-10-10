package io.agentguard.app.adapter.mcp;

import io.agentguard.app.adapter.sandbox.SandboxCommandBuilder;
import io.agentguard.app.adapter.sandbox.SandboxCommandBuilder.DockerInvocation;
import io.agentguard.app.config.AgentGuardProperties;
import io.agentguard.core.model.McpServerRef;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpClientTransport;
import java.net.URI;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Factory: builds the right MCP client transport for a server, sandboxing third-party stdio. */
@Component
public class TransportFactory {

  private final SandboxCommandBuilder sandbox;
  private final AgentGuardProperties props;

  public TransportFactory(SandboxCommandBuilder sandbox, AgentGuardProperties props) {
    this.sandbox = sandbox;
    this.props = props;
  }

  /** A transport plus the sandbox container it starts, if any (to hard-kill on timeout). */
  public record Created(McpClientTransport transport, Optional<String> sandboxContainer) {}

  public Created create(McpServerRef ref) {
    return switch (ref.transport()) {
      case STDIO -> stdio(ref);
      case STREAMABLE_HTTP -> new Created(streamable(ref), Optional.empty());
      case SSE -> new Created(sse(ref), Optional.empty());
    };
  }

  private Created stdio(McpServerRef ref) {
    if (!ref.thirdParty()) {
      ServerParameters p =
          ServerParameters.builder(ref.command()).args(ref.args()).env(ref.env()).build();
      return new Created(
          new StdioClientTransport(p, McpJsonDefaults.getMapper()), Optional.empty());
    }
    DockerInvocation inv = sandbox.run(ref);
    ServerParameters p = ServerParameters.builder(inv.command()).args(inv.args()).build();
    return new Created(
        new StdioClientTransport(p, McpJsonDefaults.getMapper()), Optional.of(inv.containerName()));
  }

  private McpClientTransport streamable(McpServerRef ref) {
    URI url = checkRemote(ref);
    return HttpClientStreamableHttpTransport.builder(origin(url))
        .endpoint(pathOf(url))
        .connectTimeout(props.mcp().connectTimeout())
        .build();
  }

  private McpClientTransport sse(McpServerRef ref) {
    URI url = checkRemote(ref);
    return HttpClientSseClientTransport.builder(origin(url))
        .sseEndpoint(pathOf(url))
        .connectTimeout(props.mcp().connectTimeout())
        .build();
  }

  /** Third-party remote servers may only be reached on allow-listed hosts. */
  URI checkRemote(McpServerRef ref) {
    URI url = ref.url();
    if (ref.thirdParty() && !props.remote().allowedHosts().contains(url.getHost())) {
      throw new IllegalArgumentException(
          "host '" + url.getHost() + "' is not in agentguard.remote.allowed-hosts");
    }
    return url;
  }

  static String origin(URI url) {
    return url.getScheme() + "://" + url.getRawAuthority();
  }

  static String pathOf(URI url) {
    String path = url.getRawPath() == null || url.getRawPath().isEmpty() ? "/" : url.getRawPath();
    return url.getRawQuery() == null ? path : path + "?" + url.getRawQuery();
  }
}
