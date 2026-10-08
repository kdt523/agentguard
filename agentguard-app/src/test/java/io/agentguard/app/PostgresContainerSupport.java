package io.agentguard.app;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** One shared Postgres 16 container per JVM for integration tests. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerSupport {

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16-alpine").withReuse(true);

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return POSTGRES;
  }
}
