package io.agentguard.core.port;

/** One message of a chat history; {@code toolName} is set only for tool results. */
public record ChatMessage(Role role, String content, String toolName) {

  /** Speaker of a message. */
  public enum Role {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
  }
}
