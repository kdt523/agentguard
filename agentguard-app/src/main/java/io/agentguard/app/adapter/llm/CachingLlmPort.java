package io.agentguard.app.adapter.llm;

import io.agentguard.core.port.ChatMessage;
import io.agentguard.core.port.ChatTurn;
import io.agentguard.core.port.LlmPort;
import io.agentguard.core.port.ToolSpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorator: memoises {@link LlmPort#structured} answers by a hash of (system, user, type), so the
 * same tool definition is classified once. Chat turns are never cached. Bounded LRU, thread-safe.
 */
public final class CachingLlmPort implements LlmPort {

  private final LlmPort delegate;
  private final Map<String, Object> cache;

  public CachingLlmPort(LlmPort delegate, int maxEntries) {
    this.delegate = delegate;
    this.cache =
        Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                return size() > maxEntries;
              }
            });
  }

  @Override
  public <T> T structured(String system, String user, Class<T> type) {
    String key = sha256(type.getName() + "\u0000" + system + "\u0000" + user);
    Object hit = cache.get(key);
    if (hit != null) {
      return type.cast(hit);
    }
    T value = delegate.structured(system, user, type);
    if (value != null) {
      cache.put(key, value);
    }
    return value;
  }

  @Override
  public ChatTurn chatWithTools(List<ChatMessage> history, List<ToolSpec> tools) {
    return delegate.chatWithTools(history, tools);
  }

  /** Number of cached answers (for tests and metrics). */
  public int size() {
    return cache.size();
  }

  private static String sha256(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
