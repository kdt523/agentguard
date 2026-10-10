package io.agentguard.core.model;

import java.util.List;

/** Snapshots captured from a set of servers, plus the servers that failed. */
public record CaptureResult(List<ToolSnapshot> snapshots, List<ServerError> errors) {

  public CaptureResult {
    snapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
    errors = errors == null ? List.of() : List.copyOf(errors);
  }
}
