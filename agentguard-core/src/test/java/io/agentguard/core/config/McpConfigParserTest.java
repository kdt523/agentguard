package io.agentguard.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Transport;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class McpConfigParserTest {

  private final McpConfigParser parser = new McpConfigParser(Set.of("trusted-one"));

  @Test
  void parse_stdioServer_mapsCommandArgsEnv() {
    List<McpServerRef> refs =
        parser.parse(
            """
            {"mcpServers":{"github":{"command":"npx","args":["-y","@x/server"],
              "env":{"GITHUB_TOKEN":"ghp_abc"}}}}\
            """);

    assertThat(refs)
        .singleElement()
        .satisfies(r -> assertThat(r.transport()).isEqualTo(Transport.STDIO))
        .satisfies(r -> assertThat(r.args()).containsExactly("-y", "@x/server"))
        .satisfies(r -> assertThat(r.env()).containsEntry("GITHUB_TOKEN", "ghp_abc"))
        .satisfies(r -> assertThat(r.thirdParty()).isTrue());
  }

  @Test
  void parse_urlServer_isStreamableHttp() {
    var ref = parser.parse("{\"mcpServers\":{\"r\":{\"url\":\"https://x.test/mcp\"}}}").get(0);

    assertThat(ref.transport()).isEqualTo(Transport.STREAMABLE_HTTP);
  }

  @Test
  void parse_sseUrlOrType_isSse() {
    var refs =
        parser.parse(
            """
            {"mcpServers":{"a":{"url":"https://x.test/sse"},
              "b":{"url":"https://x.test/events","type":"sse"}}}\
            """);

    assertThat(refs).extracting(McpServerRef::transport).containsOnly(Transport.SSE);
  }

  @Test
  void parse_fixtureAndTrustedKeys_areNotThirdParty() {
    var refs =
        parser.parse(
            """
            {"mcpServers":{"fixture-x":{"command":"docker"},"trusted-one":{"command":"node"},
              "other":{"command":"node"}}}\
            """);

    assertThat(refs)
        .extracting(r -> r.key() + "=" + r.thirdParty())
        .containsExactly("fixture-x=false", "other=true", "trusted-one=false");
  }

  @Test
  void parse_missingCommandAndUrl_rejected() {
    assertThatThrownBy(() -> parser.parse("{\"mcpServers\":{\"x\":{\"args\":[]}}}"))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("'x'");
  }

  @Test
  void parse_missingMcpServers_rejected() {
    assertThatThrownBy(() -> parser.parse("{\"servers\":{}}")).isInstanceOf(ConfigException.class);
  }

  @Test
  void parse_invalidJson_rejected() {
    assertThatThrownBy(() -> parser.parse("{nope")).isInstanceOf(ConfigException.class);
  }

  @Test
  void parse_badUrl_rejected() {
    assertThatThrownBy(() -> parser.parse("{\"mcpServers\":{\"x\":{\"url\":\"not a url\"}}}"))
        .isInstanceOf(ConfigException.class);
  }

  @Test
  void parse_argsNotArray_rejected() {
    assertThatThrownBy(
            () -> parser.parse("{\"mcpServers\":{\"x\":{\"command\":\"a\",\"args\":\"b\"}}}"))
        .isInstanceOf(ConfigException.class);
  }

  @Test
  void parse_envNotObject_rejected() {
    assertThatThrownBy(
            () -> parser.parse("{\"mcpServers\":{\"x\":{\"command\":\"a\",\"env\":[1]}}}"))
        .isInstanceOf(ConfigException.class);
  }

  @Test
  void parse_serverNotObject_rejected() {
    assertThatThrownBy(() -> parser.parse("{\"mcpServers\":{\"x\":3}}"))
        .isInstanceOf(ConfigException.class);
  }

  @Test
  void parse_secretEnv_neverInErrorsOrToString() {
    var ref =
        parser
            .parse("{\"mcpServers\":{\"x\":{\"command\":\"a\",\"env\":{\"K\":\"sk-SECRET\"}}}}")
            .get(0);

    assertThat(ref.toString()).doesNotContain("sk-SECRET");
  }

  @Test
  void parse_nullArgsAndEnv_becomeEmpty() {
    var ref =
        parser
            .parse("{\"mcpServers\":{\"x\":{\"command\":\"a\",\"args\":null,\"env\":null}}}")
            .get(0);

    assertThat(ref.args().size() + ref.env().size()).isZero();
  }
}
