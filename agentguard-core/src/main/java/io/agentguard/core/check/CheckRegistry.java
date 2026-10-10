package io.agentguard.core.check;

import io.agentguard.core.check.impl.BehaviourOverrideCheck;
import io.agentguard.core.check.impl.CodeExecutionCheck;
import io.agentguard.core.check.impl.CrossToolReferenceCheck;
import io.agentguard.core.check.impl.DescriptionLengthCheck;
import io.agentguard.core.check.impl.DestructiveWithoutHintCheck;
import io.agentguard.core.check.impl.EncodedPayloadCheck;
import io.agentguard.core.check.impl.HiddenInstructionCheck;
import io.agentguard.core.check.impl.InsecureTransportCheck;
import io.agentguard.core.check.impl.InstructionInSchemaCheck;
import io.agentguard.core.check.impl.InvisibleCharsCheck;
import io.agentguard.core.check.impl.MissingAnnotationsCheck;
import io.agentguard.core.check.impl.SecrecyCheck;
import io.agentguard.core.check.impl.SecretInConfigCheck;
import io.agentguard.core.check.impl.SensitiveResourceCheck;
import io.agentguard.core.check.impl.SuspiciousParameterCheck;
import io.agentguard.core.check.impl.ToolNameCollisionCheck;
import io.agentguard.core.check.impl.UnboundedParameterCheck;
import io.agentguard.core.check.impl.UnpinnedLaunchCheck;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The set of checks that make up a scan. Ids are unique; order is by id for determinism. */
public final class CheckRegistry {

  private final List<Check> checks;

  public CheckRegistry(List<Check> checks) {
    Set<String> ids = new HashSet<>();
    for (Check c : checks) {
      if (!ids.add(c.id())) {
        throw new IllegalArgumentException("duplicate check id " + c.id());
      }
    }
    List<Check> sorted = new ArrayList<>(checks);
    sorted.sort(Comparator.comparing(Check::id));
    this.checks = List.copyOf(sorted);
  }

  /** Every built-in check. */
  public static CheckRegistry defaults() {
    return new CheckRegistry(
        List.of(
            new HiddenInstructionCheck(),
            new SecrecyCheck(),
            new SensitiveResourceCheck(),
            new InvisibleCharsCheck(),
            new EncodedPayloadCheck(),
            new DescriptionLengthCheck(),
            new CrossToolReferenceCheck(),
            new BehaviourOverrideCheck(),
            new ToolNameCollisionCheck(),
            new CodeExecutionCheck(),
            new DestructiveWithoutHintCheck(),
            new MissingAnnotationsCheck(),
            new UnboundedParameterCheck(),
            new InstructionInSchemaCheck(),
            new SuspiciousParameterCheck(),
            new UnpinnedLaunchCheck(),
            new SecretInConfigCheck(),
            new InsecureTransportCheck()));
  }

  /** A registry with {@code extra} checks added to this one. */
  public CheckRegistry with(List<Check> extra) {
    List<Check> all = new ArrayList<>(checks);
    all.addAll(extra);
    return new CheckRegistry(all);
  }

  public List<Check> all() {
    return checks;
  }

  public List<Check> withScope(Scope scope) {
    return checks.stream().filter(c -> c.scope() == scope).toList();
  }

  public Optional<Check> byId(String id) {
    return checks.stream().filter(c -> c.id().equals(id)).findFirst();
  }
}
