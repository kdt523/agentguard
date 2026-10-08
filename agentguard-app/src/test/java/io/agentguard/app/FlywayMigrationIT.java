package io.agentguard.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(PostgresContainerSupport.class)
class FlywayMigrationIT {

  private static final List<String> TABLES =
      List.of(
          "servers",
          "snapshots",
          "tools",
          "scans",
          "findings",
          "flows",
          "tool_pins",
          "drift_events",
          "redteam_runs",
          "redteam_cases");

  @Autowired JdbcTemplate jdbc;

  @Test
  void contextStarts_flywayApplied_allTablesExist() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'",
            String.class);

    assertThat(tables).containsAll(TABLES);
  }
}
