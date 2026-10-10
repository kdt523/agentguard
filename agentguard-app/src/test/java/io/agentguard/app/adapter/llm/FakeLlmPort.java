package io.agentguard.app.adapter.llm;

import io.agentguard.core.port.ChatMessage;
import io.agentguard.core.port.ChatTurn;
import io.agentguard.core.port.LlmPort;
import io.agentguard.core.port.ToolSpec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Scripted LLM for tests: returns queued answers in order and records every prompt. */
public final class FakeLlmPort implements LlmPort {

  private final Deque<Object> answers = new ArrayDeque<>();
  private final List<String> userPrompts = new ArrayList<>();
  private int calls;

  public FakeLlmPort answer(Object value) {
    answers.add(value);
    return this;
  }

  public int calls() {
    return calls;
  }

  public List<String> userPrompts() {
    return userPrompts;
  }

  @Override
  public <T> T structured(String system, String user, Class<T> type) {
    calls++;
    userPrompts.add(user);
    return type.cast(answers.removeFirst());
  }

  @Override
  public ChatTurn chatWithTools(List<ChatMessage> history, List<ToolSpec> tools) {
    calls++;
    return (ChatTurn) answers.removeFirst();
  }
}
