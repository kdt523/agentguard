package io.agentguard.app.adapter.sandbox;

import io.agentguard.app.config.AgentGuardProperties;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Hard-kills sandbox containers ({@code docker rm -f}); idempotent and never throws. */
@Component
public class SandboxReaper {

  private static final Logger log = LoggerFactory.getLogger(SandboxReaper.class);
  private final String docker;

  public SandboxReaper(AgentGuardProperties props) {
    this.docker = props.sandbox().dockerCommand();
  }

  public void kill(String containerName) {
    try {
      Process p =
          new ProcessBuilder(List.of(docker, "rm", "-f", containerName))
              .redirectErrorStream(true)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .start();
      if (!p.waitFor(15, TimeUnit.SECONDS)) {
        p.destroyForcibly();
      }
    } catch (IOException e) {
      log.warn("could not remove sandbox container {}: {}", containerName, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
