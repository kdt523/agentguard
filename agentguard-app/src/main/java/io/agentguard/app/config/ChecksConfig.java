package io.agentguard.app.config;

import io.agentguard.core.check.Check;
import io.agentguard.core.check.CheckEngine;
import io.agentguard.core.check.CheckRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the built-in checks plus any extra {@link Check} beans, and the engine that runs them.
 */
@Configuration(proxyBeanMethods = false)
public class ChecksConfig {

  @Bean
  CheckRegistry checkRegistry(ObjectProvider<Check> extraChecks) {
    return CheckRegistry.defaults().with(extraChecks.orderedStream().toList());
  }

  @Bean
  CheckEngine checkEngine(CheckRegistry registry) {
    return new CheckEngine(registry);
  }
}
