package io.agentguard.core.port;

import java.util.List;

/** Provider-neutral access to an LLM. */
public interface LlmPort {
  /** Asks for a JSON answer mapped onto {@code type}. */
  <T> T structured(String system, String user, Class<T> type);

  /** One chat turn in which the model may call the given tools (red-team harness). */
  ChatTurn chatWithTools(List<ChatMessage> history, List<ToolSpec> tools);
}
