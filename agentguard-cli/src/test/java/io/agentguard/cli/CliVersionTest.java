package io.agentguard.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CliVersionTest {

  @Test
  void version_printsProjectVersion_exitsZero() {
    StringWriter out = new StringWriter();
    CommandLine cmd = new CommandLine(new AgentGuardCli());
    cmd.setOut(new PrintWriter(out));

    int code = cmd.execute("--version");

    assertThat(code).isZero();
    assertThat(out.toString()).startsWith("agentguard ").doesNotContain("unknown");
  }

  @Test
  void execute_noArgs_printsUsageAndExitsZero() {
    assertThat(AgentGuardCli.execute()).isZero();
  }

  @Test
  void execute_unknownOption_exitsWithUsageError() {
    assertThat(AgentGuardCli.execute("--bogus")).isEqualTo(2);
  }
}
