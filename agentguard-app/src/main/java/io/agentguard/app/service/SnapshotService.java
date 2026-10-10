package io.agentguard.app.service;

import io.agentguard.app.adapter.persistence.JpaSnapshotRepositoryAdapter;
import io.agentguard.app.config.AgentGuardProperties;
import io.agentguard.core.model.CaptureResult;
import io.agentguard.core.model.McpServerRef;
import io.agentguard.core.model.ServerError;
import io.agentguard.core.model.ToolSnapshot;
import io.agentguard.core.port.McpSnapshotPort;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Use case: fetch snapshots of many servers in parallel (virtual threads) and persist them. */
@Service
public class SnapshotService {

  private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);
  private final McpSnapshotPort mcp;
  private final JpaSnapshotRepositoryAdapter repository;
  private final AgentGuardProperties props;

  public SnapshotService(
      McpSnapshotPort mcp, JpaSnapshotRepositoryAdapter repository, AgentGuardProperties props) {
    this.mcp = mcp;
    this.repository = repository;
    this.props = props;
  }

  /** Captures and stores all servers; one failing server becomes a {@link ServerError}. */
  public CaptureResult capture(List<McpServerRef> refs) {
    CaptureResult fetched = fetchAll(refs);
    for (McpServerRef ref : refs) {
      repository.registerServer(ref);
    }
    fetched.snapshots().forEach(repository::save);
    return fetched;
  }

  /** Fetches all servers in parallel without persisting (used by the local CLI too). */
  public CaptureResult fetchAll(List<McpServerRef> refs) {
    Duration request = props.mcp().requestTimeout();
    Duration overall = request.plus(props.mcp().connectTimeout()).plusSeconds(5);
    Map<McpServerRef, Future<ToolSnapshot>> futures = new LinkedHashMap<>();
    List<ToolSnapshot> ok = new ArrayList<>();
    List<ServerError> errors = new ArrayList<>();
    try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
      for (McpServerRef ref : refs) {
        futures.put(ref, exec.submit(() -> mcp.fetch(ref, request)));
      }
      futures.forEach(
          (ref, f) -> {
            try {
              ok.add(f.get(overall.toMillis(), TimeUnit.MILLISECONDS));
            } catch (TimeoutException e) {
              f.cancel(true);
              errors.add(
                  new ServerError(ref.key(), "timed out after " + overall.toSeconds() + "s"));
            } catch (ExecutionException e) {
              errors.add(new ServerError(ref.key(), rootMessage(e)));
              log.warn("fetch failed for server {}", ref.key(), e.getCause());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              errors.add(new ServerError(ref.key(), "interrupted"));
            }
          });
    }
    return new CaptureResult(ok, errors);
  }

  private static String rootMessage(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null && t.getCause() != t) {
      t = t.getCause();
    }
    String m = t.getMessage();
    return t.getClass().getSimpleName() + (m == null ? "" : ": " + m);
  }
}
