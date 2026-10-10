package io.agentguard.app.config;

import io.agentguard.core.config.McpConfigParser;
import io.agentguard.core.port.ClockPort;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires framework-free core components as Spring beans. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentGuardProperties.class)
public class CoreBeansConfig {

  @Bean
  ClockPort clockPort() {
    Clock clock = Clock.systemUTC();
    return clock::instant;
  }

  @Bean
  McpConfigParser mcpConfigParser(AgentGuardProperties props) {
    return new McpConfigParser(props.trustedServers());
  }
}
