package io.agentguard.core.port;

import java.util.List;

/** The model's reply: text and any tool calls it requested. */
public record ChatTurn(String text, List<ToolCall> toolCalls) {

  public ChatTurn {
    text = text == null ? "" : text;
    toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
  }
}
