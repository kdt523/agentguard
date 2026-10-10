package io.agentguard.core.check.impl;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckContext;
import io.agentguard.core.check.FindingBuilder;
import io.agentguard.core.check.Scope;
import io.agentguard.core.model.Evidence;
import io.agentguard.core.model.Finding;
import io.agentguard.core.model.Severity;
import io.agentguard.core.model.ToolDefinition;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** AG-PE-001: the tool can run commands or code (shell, exec, eval, run-command parameters). */
public final class CodeExecutionCheck implements Check {

  private static final Set<String> NAME_TOKENS =
      Set.of("shell", "exec", "execute", "eval", "bash", "powershell", "subprocess", "terminal");
  private static final Set<String> PARAMS = Set.of("cmd", "command", "script", "shell_command");
  private static final Pattern DESCRIPTION =
      Pattern.compile(
          "\\b(shell|bash|powershell|subprocess|arbitrary\\s+(command|code|script)s?"
              + "|(execute|run)s?\\s+(a\\s+|an\\s+|any\\s+|the\\s+)?(shell\\s+|system\\s+|terminal\\s+)?"
              + "(command|script|code))\\b",
          Pattern.CASE_INSENSITIVE);

  @Override
  public String id() {
    return "AG-PE-001";
  }

  @Override
  public String title() {
    return "Tool can execute commands or code";
  }

  @Override
  public String owaspRef() {
    return "MCP05";
  }

  @Override
  public Severity defaultSeverity() {
    return Severity.HIGH;
  }

  @Override
  public Scope scope() {
    return Scope.TOOL;
  }

  @Override
  public List<Finding> run(CheckContext ctx) {
    ToolDefinition tool = ctx.tool();
    if (tool == null) {
      return List.of();
    }
    FindingBuilder b = FindingBuilder.of(this, ctx);
    boolean strong = false;
    for (String token : ToolText.nameTokens(tool.name())) {
      if (NAME_TOKENS.contains(token)) {
        b.evidence(new Evidence("name", tool.name(), 0, tool.name().length()));
        strong = true;
        break;
      }
    }
    Matcher m = DESCRIPTION.matcher(tool.description());
    if (m.find()) {
      b.evidence("description", tool.description(), m.start(), m.end());
      strong = true;
    }
    boolean param = false;
    for (String p : ToolText.params(tool).keySet()) {
      if (PARAMS.contains(p.toLowerCase())) {
        b.evidence(
            new Evidence("inputSchema.properties." + p, "parameter '" + p + "'", 0, p.length()));
        param = true;
      }
    }
    if (!strong && !param) {
      return List.of();
    }
    return List.of(
        b.confidence(strong ? 0.9 : 0.6)
            .remediation(
                "Avoid exposing generic command execution to the model. Require human approval for"
                    + " each call, run it in a sandbox, or replace it with narrowly scoped tools.")
            .build());
  }
}
