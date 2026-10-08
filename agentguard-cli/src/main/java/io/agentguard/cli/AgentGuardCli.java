package io.agentguard.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** Root command of the AgentGuard CLI. Subcommands are added in later phases. */
@Command(
    name = "agentguard",
    mixinStandardHelpOptions = true,
    versionProvider = AgentGuardCli.VersionProvider.class,
    description = "Security scanner for AI agents and MCP servers.")
public class AgentGuardCli implements Runnable {

  @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  public static void main(String[] args) {
    System.exit(execute(args));
  }

  /** Runs the CLI and returns the exit code (testable without System.exit). */
  public static int execute(String... args) {
    return new CommandLine(new AgentGuardCli()).execute(args);
  }

  @Override
  public void run() {
    spec.commandLine().usage(spec.commandLine().getOut());
  }

  /** Reads the project version filtered into the classpath at build time. */
  static final class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() throws IOException {
      Properties props = new Properties();
      try (InputStream in =
          AgentGuardCli.class.getResourceAsStream("/agentguard-version.properties")) {
        if (in != null) {
          props.load(in);
        }
      }
      return new String[] {"agentguard " + props.getProperty("version", "unknown")};
    }
  }
}
