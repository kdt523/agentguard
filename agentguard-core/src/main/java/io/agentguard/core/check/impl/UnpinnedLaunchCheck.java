package io.agentguard.core.check.impl;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.FindingBuilder;
import io.agentguard.core.check.Scope;
import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.Transport;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** AG-CF-001: the server is launched via npx/uvx/pipx without pinning an exact package version. */
public final class UnpinnedLaunchCheck implements Check {

  private static final Set<String> NODE_RUNNERS = Set.of("npx", "bunx", "pnpx");
  private static final Set<String> PY_RUNNERS = Set.of("uvx", "pipx");
  private static final Pattern NODE_PINNED =
      Pattern.compile("^(@[^/@]+/)?[^@/]+@(?!latest$|next$|\\*$)[^@]+$");

  @Override
  public String id() {
    return "AG-CF-001";
  }

  @Override
  public String title() {
    return "Package launched without a pinned version";
  }

  @Override
  public String owaspRef() {
    return "MCP04";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.MEDIUM;
  }

  @Override
  public Scope scope() {
    return Scope.SERVER;
  }

  @Override
  public List<Finding> run(CheckContext ctx) {
    Optional<McpServerRef> ref = ctx.server(ctx.snapshot().serverKey());
    if (ref.isEmpty() || ref.get().transport() != Transport.STDIO) {
      return List.of();
    }
    McpServerRef r = ref.get();
    String runner = baseName(r.command());
    String pkg = firstPositional(r.args());
    if (pkg == null || !isUnpinned(runner, pkg)) {
      return List.of();
    }
    String text = r.command() + " " + String.join(" ", r.args());
    return List.of(
        FindingBuilder.of(this, ctx)
            .confidence(0.9)
            .evidence(new Evidence("command", text, 0, text.length()))
            .remediation(
                "Pin an exact version (npx pkg@1.2.3, uvx pkg==1.2.3) so the code you reviewed is"
                    + " the code that runs, and commit a lockfile.")
            .build());
  }

  private static boolean isUnpinned(String runner, String pkg) {
    if (NODE_RUNNERS.contains(runner)) {
      return !NODE_PINNED.matcher(pkg).matches();
    }
    if (PY_RUNNERS.contains(runner)) {
      return !(pkg.contains("==") || pkg.contains("@"));
    }
    return false;
  }

  private static String firstPositional(List<String> args) {
    for (String a : args) {
      if (!a.startsWith("-")) {
        return a;
      }
    }
    return null;
  }

  private static String baseName(String command) {
    String c = command.replace('\\', '/');
    return c.substring(c.lastIndexOf('/') + 1)
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\.(exe|cmd)$", "");
  }
}
