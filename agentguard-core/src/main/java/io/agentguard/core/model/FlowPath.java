package io.agentguard.core.model;

import java.util.List;

/** A toxic data-flow path; tool ids are {@code serverKey + "/" + toolName}. */
public record FlowPath(List<String> toolIds, double risk, String narrative) {

  public FlowPath {
    toolIds = toolIds == null ? List.of() : List.copyOf(toolIds);
    narrative = narrative == null ? "" : narrative;
  }
}
