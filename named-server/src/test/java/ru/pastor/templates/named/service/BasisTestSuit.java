package ru.pastor.templates.named.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.configuration.TestConfiguration;

@SuppressWarnings("AbstractClassName")
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
  properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.r2dbc.username=sa",
    "spring.r2dbc.password=",
    "spring.r2dbc.pool.validation-query=SELECT 1",
    "spring.application.name=named-count-test",
    "spring.data.redis.host=localhost",
    "grpc.server.in-process-name=test",
    "grpc.server.port=-1",
    "grpc.client.inProcess.address=in-process:test"
  }
)
@SpringJUnitConfig(
  classes = TestConfiguration.class
)
public abstract class BasisTestSuit {

  @Autowired
  protected DatabaseClient databaseClient;

  @BeforeEach
  protected void setUp() {
    if (databaseClient != null) {
      Mono.from(databaseClient.sql("CREATE SCHEMA IF NOT EXISTS named").then()).block();
      // Create counter_catalogue table
      Mono.from(databaseClient.sql(
        "CREATE TABLE IF NOT EXISTS named.counter_catalogue (" +
          "  id SERIAL PRIMARY KEY," +
          "  name VARCHAR NOT NULL," +
          "  description VARCHAR NOT NULL," +
          "  default_value BIGINT NOT NULL DEFAULT 0," +
          "  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
          "  updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
          "  CONSTRAINT unique_name UNIQUE (name)" +
          ")"
      ).then()).block();

      // Create counter_values table
      Mono.from(databaseClient.sql(
        "CREATE TABLE IF NOT EXISTS named.counter_values (" +
          "  id SERIAL PRIMARY KEY," +
          "  counter_id BIGINT NOT NULL," +
          "  user_id BIGINT NOT NULL," +
          "  \"value\" BIGINT NOT NULL," +
          "  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
          "  updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
          "  CONSTRAINT unique_counter_user UNIQUE (counter_id, user_id)," +
          "  CONSTRAINT fk_counter FOREIGN KEY (counter_id) REFERENCES named.counter_catalogue(id)" +
          ")"
      ).then()).block();
    } else {
      System.out.println("[DEBUG_LOG] DatabaseClient is null in setUp");
    }
  }

  @AfterEach
  protected void tearAllDown() {
    if (databaseClient != null) {
      // Drop tables and schema
      Mono.from(databaseClient.sql(
        "DROP TABLE IF EXISTS named.counter_values"
      ).then()).block();

      Mono.from(databaseClient.sql(
        "DROP TABLE IF EXISTS named.counter_catalogue"
      ).then()).block();
    } else {
      System.out.println("[DEBUG_LOG] DatabaseClient is null in tearAllDown");
    }
  }
}
