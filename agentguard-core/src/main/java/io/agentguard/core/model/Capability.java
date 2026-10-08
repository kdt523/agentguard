package io.agentguard.core.model;

/** Security-relevant capability of a tool, used by the toxic-flow graph. */
public enum Capability {
  /** Reads content an attacker can control: web pages, email, issues, chat, others' files. */
  UNTRUSTED_INPUT,
  /** Reads secrets, local files, DB rows, credentials, personal data. */
  PRIVATE_DATA,
  /** Sends data out: http post, email send, chat post, git push, webhook, upload. */
  EXTERNAL_SINK,
  /** Deletes or modifies state. */
  DESTRUCTIVE,
  /** Runs commands or code. */
  CODE_EXEC
}
