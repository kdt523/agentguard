package io.agentguard.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.agentguard.core.port.ChatTurn;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModelInvariantsTest {

  @Test
  void serverRef_toString_neverContainsEnvValues() {
    var ref =
        new McpServerRef(
            "gh", Transport.STDIO, "npx", List.of("-y"), Map.of("TOKEN", "ghp_secret"), null, true);

    assertThat(ref.toString()).contains("TOKEN").doesNotContain("ghp_secret");
  }

  @Test
  void serverRef_stdioWithoutCommand_isRejected() {
    assertThatThrownBy(() -> new McpServerRef("x", Transport.STDIO, " ", null, null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void serverRef_remoteWithoutUrl_isRejected() {
    assertThatThrownBy(
            () -> new McpServerRef("x", Transport.STREAMABLE_HTTP, null, null, null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void serverRef_optionalAccessors_reflectTransport() {
    var remote =
        new McpServerRef(
            "r", Transport.SSE, null, null, null, URI.create("https://x.test/mcp"), true);

    assertThat(remote)
        .satisfies(r -> assertThat(r.commandOpt()).isEmpty())
        .satisfies(r -> assertThat(r.urlOpt()).contains(URI.create("https://x.test/mcp")))
        .satisfies(r -> assertThat(r.args()).isEmpty())
        .satisfies(r -> assertThat(r.env()).isEmpty());
  }

  @Test
  void toolDefinition_nullFields_areNormalised() {
    var tool = new ToolDefinition("add", null, null, null, null);

    assertThat(tool)
        .satisfies(t -> assertThat(t.description()).isEmpty())
        .satisfies(t -> assertThat(t.inputSchema().isObject()).isTrue())
        .satisfies(t -> assertThat(t.annotationsOpt()).isEmpty())
        .satisfies(t -> assertThat(t.sha256()).isEmpty());
  }

  @Test
  void finding_scoreAndConfidence_areClamped() {
    var f = new Finding("AG-TP-001", null, null, Severity.HIGH, 500, "s", null, null, null, 7.0);

    assertThat(f)
        .satisfies(x -> assertThat(x.score()).isEqualTo(100))
        .satisfies(x -> assertThat(x.confidence()).isEqualTo(1.0))
        .satisfies(x -> assertThat(x.toolNameOpt()).isEmpty())
        .satisfies(x -> assertThat(x.evidence()).isEmpty());
  }

  @Test
  void finding_negativeValues_clampToMinimum() {
    var f = new Finding("AG-TP-001", "", "", Severity.LOW, -3, "s", "t", List.of(), "", -1);

    assertThat(List.of(f.score(), (int) f.confidence())).containsExactly(1, 0);
  }

  @Test
  void evidence_longExcerpt_isTruncatedTo200() {
    var e = new Evidence(null, "x".repeat(500), 0, 500);

    assertThat(e.excerpt()).hasSize(Evidence.MAX_EXCERPT);
  }

  @Test
  void evidence_nulls_becomeEmpty() {
    var e = new Evidence(null, null, 0, 0);

    assertThat(e.field() + e.excerpt()).isEmpty();
  }

  @Test
  void severity_atLeast_ordersBySeverity() {
    assertThat(
            List.of(Severity.CRITICAL.atLeast(Severity.HIGH), Severity.LOW.atLeast(Severity.HIGH)))
        .containsExactly(true, false);
  }

  @Test
  void snapshot_nullCollections_becomeEmpty() {
    var s = new ToolSnapshot("k", null, null, Instant.EPOCH, null, null, null, null);

    assertThat(s.tools().size() + s.prompts().size() + s.resources().size()).isZero();
  }

  @Test
  void scanResult_nullParts_haveSafeDefaults() {
    var r = new ScanResult(UUID.randomUUID(), null, null, null, null, null);

    assertThat(r.cut()).isEqualTo(CutRecommendation.none());
  }

  @Test
  void records_nonNullInputs_arePreserved() {
    var schema =
        tools.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("type", "object");
    var ann = new ToolAnnotations(true, false, null, null, "Add");
    var tool = new ToolDefinition("add", "Adds", schema, ann, "abc");
    var stdio =
        new McpServerRef("k", Transport.STDIO, "python", List.of("s.py"), Map.of(), null, false);
    var snap =
        new ToolSnapshot(
            "k",
            "n",
            "1",
            Instant.EPOCH,
            List.of(tool),
            List.of(new PromptDefinition("p", "d")),
            List.of(new ResourceDefinition("u", "n", "d", "text/plain")),
            "h");
    var cut = new CutRecommendation(List.of("a/b"), 1.0);
    var result = new ScanResult(UUID.randomUUID(), List.of(snap), List.of(), List.of(), cut, null);

    assertThat(tool.annotationsOpt()).contains(ann);
    assertThat(stdio.commandOpt()).contains("python");
    assertThat(snap.serverName() + snap.serverVersion() + snap.snapshotSha256()).isEqualTo("n1h");
    assertThat(result.cut()).isSameAs(cut);
    assertThat(new Evidence("f", "short", 0, 5).excerpt()).isEqualTo("short");
    assertThat(new ChatTurn("hi", List.of()).text()).isEqualTo("hi");
  }

  @Test
  void smallRecords_nullCollections_becomeEmpty() {
    assertThat(new FlowPath(null, 0, null).toolIds()).isEmpty();
    assertThat(new CutRecommendation(null, 0).toolIds()).isEmpty();
    assertThat(new ScoreBreakdown(0, "LOW", null).components()).isEmpty();
    assertThat(new PromptDefinition("p", null).description()).isEmpty();
    assertThat(new ResourceDefinition("u", "n", null, null).description()).isEmpty();
    assertThat(new ChatTurn(null, null).toolCalls()).isEmpty();
  }
}
