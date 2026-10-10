package io.agentguard.core.check;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolAnnotations;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/** Adding benign English sentences to a clean description must never produce a HIGH+ finding. */
class BenignSentenceProperty {

  private static final List<String> BENIGN =
      List.of(
          "Returns the result as a JSON object.",
          "This tool is useful for quick calculations.",
          "Supports integers and decimals.",
          "The response is cached for five minutes.",
          "Results are sorted alphabetically.",
          "Use the optional limit to cap the number of rows.",
          "Time zones follow the IANA database.",
          "Handles unicode text correctly, including accents.",
          "Please see the documentation for examples.",
          "Rate limits apply to heavy usage.",
          "Useful when you want a short summary of a page.",
          "Returns an empty list when nothing matches.",
          "Names are case sensitive.",
          "The output format is stable across versions.");

  private final CheckEngine engine = new CheckEngine(CheckRegistry.defaults());

  @Provide
  Arbitrary<List<String>> sentences() {
    return Arbitraries.of(BENIGN).list().ofMinSize(0).ofMaxSize(6);
  }

  @Property(tries = 300, seed = "42")
  void benignSentences_neverProduceHighOrCritical(@ForAll("sentences") List<String> extra) {
    String desc = "Add two numbers and return the sum. " + String.join(" ", extra);
    var tool =
        new ToolDefinition(
            "add", desc, null, new ToolAnnotations(true, false, true, false, null), "");
    var snap = new ToolSnapshot("s1", "s1", "1", Instant.EPOCH, List.of(tool), null, null, "h");

    List<Finding> findings = engine.scan(List.of(snap), Map.of());

    assertThat(findings).noneMatch(f -> f.severity().atLeast(Severity.HIGH));
  }
}
