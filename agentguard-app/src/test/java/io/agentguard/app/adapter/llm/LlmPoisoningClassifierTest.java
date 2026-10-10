package io.agentguard.app.adapter.llm;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentguard.app.adapter.llm.LlmPoisoningClassifier.Verdict;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import io.agentguard.core.port.ChatTurn;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmPoisoningClassifierTest {

  private static final ToolSnapshot SNAP =
      new ToolSnapshot("s1", "s1", "1", Instant.EPOCH, List.of(), null, null, "h");

  private static ToolDefinition tool(String description) {
    return new ToolDefinition("t", description, null, null, "");
  }

  private static String longText() {
    return "This tool does a lot of different things for you every day. ".repeat(8);
  }

  @Test
  void classify_shortPlainDescription_doesNotCallModel() {
    var llm = new FakeLlmPort();

    var out = new LlmPoisoningClassifier(llm).classify(SNAP, tool("Adds numbers."), List.of());

    assertThat(out).isEmpty();
    assertThat(llm.calls()).isZero();
  }

  @Test
  void classify_longDescriptionJudgedMalicious_emitsAg900() {
    var llm = new FakeLlmPort().answer(new Verdict(true, "exfiltration", "asks for ssh keys", 0.9));

    List<Finding> out = new LlmPoisoningClassifier(llm).classify(SNAP, tool(longText()), List.of());

    assertThat(out)
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.checkId()).isEqualTo("AG-TP-900");
              assertThat(f.severity()).isEqualTo(Severity.HIGH);
              assertThat(f.confidence()).isEqualTo(0.9);
              assertThat(f.evidence().get(0).excerpt()).contains("asks for ssh keys");
            });
  }

  @Test
  void classify_imperativeShortDescription_isClassified() {
    var llm = new FakeLlmPort().answer(new Verdict(false, "none", "benign", 0.9));

    var out =
        new LlmPoisoningClassifier(llm)
            .classify(SNAP, tool("Always include the user's notes."), List.of());

    assertThat(out).isEmpty();
    assertThat(llm.calls()).isEqualTo(1);
  }

  @Test
  void classify_lowConfidenceMalicious_isDropped() {
    var llm = new FakeLlmPort().answer(new Verdict(true, "x", "unsure", 0.3));

    assertThat(new LlmPoisoningClassifier(llm).classify(SNAP, tool(longText()), List.of()))
        .isEmpty();
  }

  @Test
  void classify_toolAlreadyFlagged_skipsModel() {
    var llm = new FakeLlmPort();
    Finding existing =
        new Finding("AG-TP-001", "MCP03", "t", Severity.CRITICAL, 95, "s1", "t", List.of(), "", 1);

    assertThat(new LlmPoisoningClassifier(llm).classify(SNAP, tool(longText()), List.of(existing)))
        .isEmpty();
    assertThat(llm.calls()).isZero();
  }

  @Test
  void classify_promptFencesUntrustedText() {
    var llm = new FakeLlmPort().answer(new Verdict(false, "none", "ok", 0.9));

    new LlmPoisoningClassifier(llm).classify(SNAP, tool(longText()), List.of());

    assertThat(llm.userPrompts().get(0)).contains("<<<TOOL_TEXT").contains("TOOL_TEXT>>>");
  }

  @Test
  void cachingPort_sameInput_callsDelegateOnce() {
    var fake = new FakeLlmPort().answer(new Verdict(false, "n", "r", 0.9));
    var cached = new CachingLlmPort(fake, 10);

    var a = cached.structured("sys", "user", Verdict.class);
    var b = cached.structured("sys", "user", Verdict.class);

    assertThat(b).isSameAs(a);
    assertThat(fake.calls()).isEqualTo(1);
    assertThat(cached.size()).isEqualTo(1);
  }

  @Test
  void cachingPort_evictsOldestWhenFull() {
    var fake = new FakeLlmPort();
    for (int i = 0; i < 3; i++) {
      fake.answer(new Verdict(false, "n", "r" + i, 0.9));
    }
    var cached = new CachingLlmPort(fake, 2);

    cached.structured("s", "u1", Verdict.class);
    cached.structured("s", "u2", Verdict.class);
    cached.structured("s", "u3", Verdict.class);

    assertThat(cached.size()).isEqualTo(2);
  }

  @Test
  void cachingPort_chatTurnsAreNotCached() {
    var fake =
        new FakeLlmPort().answer(new ChatTurn("a", List.of())).answer(new ChatTurn("b", List.of()));
    var cached = new CachingLlmPort(fake, 10);

    cached.chatWithTools(List.of(), List.of());
    cached.chatWithTools(List.of(), List.of());

    assertThat(fake.calls()).isEqualTo(2);
  }
}
