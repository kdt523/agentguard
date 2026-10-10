package io.agentguard.core.check;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentguard.core.model.Finding;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Runs every YAML case file against its check and enforces >= 3 positive and >= 3 negative cases.
 */
class CheckTableTest {

  private final CheckRegistry registry = CheckRegistry.defaults();

  @TestFactory
  Stream<DynamicTest> yamlCases_produceExpectedFindings() throws IOException {
    List<DynamicTest> tests = new java.util.ArrayList<>();
    for (Path file : CheckCaseLoader.files()) {
      CheckCaseLoader.CaseFile cf = CheckCaseLoader.load(file);
      Check check = registry.byId(cf.checkId()).orElseThrow();
      for (CheckCaseLoader.Case c : cf.cases()) {
        tests.add(DynamicTest.dynamicTest(cf.checkId() + " :: " + c.name(), () -> run(check, c)));
      }
    }
    return tests.stream();
  }

  private static void run(Check check, CheckCaseLoader.Case c) {
    CheckEngine engine = new CheckEngine(new CheckRegistry(List.of(check)));

    List<Finding> findings = engine.scan(c.snapshots(), c.servers());

    assertThat(findings)
        .as("check crashed: %s", findings)
        .noneMatch(f -> f.checkId().equals(CheckEngine.ENGINE_FAILURE_ID));
    assertThat(!findings.isEmpty())
        .as("finding expected=%s: %s", c.expectFinding(), findings)
        .isEqualTo(c.expectFinding());
    if (c.expectSeverity() != null) {
      assertThat(findings).extracting(Finding::severity).contains(c.expectSeverity());
    }
  }

  @Test
  void everyCheck_hasAtLeastThreePositiveAndThreeNegativeCases() throws IOException {
    for (Check check : registry.all()) {
      Path file = Path.of("src", "test", "resources", "checks", check.id() + ".yaml");
      assertThat(file).as("case file for %s", check.id()).exists();
      CheckCaseLoader.CaseFile cf = CheckCaseLoader.load(file);
      long pos = cf.cases().stream().filter(CheckCaseLoader.Case::expectFinding).count();
      long neg = cf.cases().size() - pos;
      assertThat(pos).as("%s positive cases", check.id()).isGreaterThanOrEqualTo(3);
      assertThat(neg).as("%s negative cases", check.id()).isGreaterThanOrEqualTo(3);
    }
  }
}
