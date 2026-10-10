package io.agentguard.core.model;

/** A server that could not be fetched; one failing server never fails a whole scan. */
public record ServerError(String serverKey, String message) {

  public ServerError {
    message = message == null ? "" : message;
  }
}
