package io.agentguard.core.model;

import java.util.List;
import java.util.UUID;

/** Complete outcome of one scan. */
public record ScanResult(
    UUID scanId,
    List<ToolSnapshot> snapshots,
    List<Finding> findings,
    List<FlowPath> flows,
    CutRecommendation cut,
    ScoreBreakdown score) {

  public ScanResult {
    snapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
    findings = findings == null ? List.of() : List.copyOf(findings);
    flows = flows == null ? List.of() : List.copyOf(flows);
    cut = cut == null ? CutRecommendation.none() : cut;
  }
}
