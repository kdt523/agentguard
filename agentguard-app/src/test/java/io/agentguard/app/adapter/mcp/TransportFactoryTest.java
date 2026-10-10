package io.agentguard.app.adapter.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.agentguard.app.adapter.sandbox.SandboxCommandBuilder;
import io.agentguard.app.config.AgentGuardProperties;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Transport;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransportFactoryTest {

  private final AgentGuardProperties props =
      new AgentGuardProperties(
          null, null, null, new AgentGuardProperties.Remote(List.of("allowed.test")));
  private final TransportFactory factory =
      new TransportFactory(new SandboxCommandBuilder(props), props);

  private static McpServerRef remote(Transport t, String url, boolean thirdParty) {
    return new McpServerRef("r", t, null, null, null, URI.create(url), thirdParty);
  }

  @Test
  void create_trustedStdio_runsDirectlyWithoutSandbox() {
    var ref = new McpServerRef("fixture-a", Transport.STDIO, "python", null, null, null, false);

    var created = factory.create(ref);

    assertThat(created.transport()).isInstanceOf(StdioClientTransport.class);
    assertThat(created.sandboxContainer()).isEmpty();
  }

  @Test
  void create_thirdPartyStdio_isSandboxed() {
    var ref = new McpServerRef("x", Transport.STDIO, "npx", List.of("y"), Map.of(), null, true);

    assertThat(factory.create(ref).sandboxContainer())
        .hasValueSatisfying(n -> assertThat(n).startsWith("agentguard-sbx-x-"));
  }

  @Test
  void create_streamableHttp_onAllowedHost() {
    var created =
        factory.create(remote(Transport.STREAMABLE_HTTP, "https://allowed.test/mcp", true));

    assertThat(created.transport()).isInstanceOf(HttpClientStreamableHttpTransport.class);
  }

  @Test
  void create_sse_trustedAnyHost() {
    var created = factory.create(remote(Transport.SSE, "http://localhost:9/sse", false));

    assertThat(created.transport()).isInstanceOf(HttpClientSseClientTransport.class);
  }

  @Test
  void create_thirdPartyRemote_notAllowListed_rejected() {
    assertThatThrownBy(
            () -> factory.create(remote(Transport.STREAMABLE_HTTP, "https://evil.test/mcp", true)))
        .hasMessageContaining("evil.test");
  }

  @Test
  void originAndPath_splitUrl() {
    URI u = URI.create("https://h.test:8443/a/mcp?x=1");

    assertThat(List.of(TransportFactory.origin(u), TransportFactory.pathOf(u)))
        .containsExactly("https://h.test:8443", "/a/mcp?x=1");
  }

  @Test
  void pathOf_emptyPath_isSlash() {
    assertThat(TransportFactory.pathOf(URI.create("https://h.test"))).isEqualTo("/");
  }

  @Test
  void mapper_nullDescriptionAndSchema_normalised() {
    var tool = McpSchemaMapper.tool(McpSchema.Tool.builder().name("t").build());

    assertThat(tool.description()).isEmpty();
    assertThat(McpSchemaMapper.annotations(null)).isNull();
  }

  @Test
  void paged_followsCursorUntilEmpty() {
    Map<String, List<Integer>> pages = Map.of("", List.of(1, 2), "c1", List.of(3));
    List<Integer> all =
        McpClientAdapter.paged(
            c -> c == null ? "" : c, p -> pages.get(p), p -> p.isEmpty() ? "c1" : null);

    assertThat(all).containsExactly(1, 2, 3);
  }

  @Test
  void paged_endlessCursor_isBounded() {
    assertThatThrownBy(() -> McpClientAdapter.paged(c -> "x", p -> List.of(1), p -> "again"))
        .isInstanceOf(IllegalStateException.class);
  }
}
