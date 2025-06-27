package ru.pastor.templates.named.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.pastor.templates.named.service.BasisTestSuit;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//FIXME: Поправить для H2 либо как-то еще решить проблему
@Disabled
class CounterRepositoryTest extends BasisTestSuit {

  @Autowired
  @Qualifier("CounterRepository.Postgres")
  private CounterRepository counterRepository;

  @Autowired
  private DatabaseClient databaseClient;

  private Integer catalogueId;

  @BeforeEach
  void setUpTest() {
    super.setUp();
    // Clear existing data
    Mono.from(databaseClient.sql("DELETE FROM named.counter_values").then()).block();
    Mono.from(databaseClient.sql("DELETE FROM named.counter_catalogue").then()).block();

    // Insert test catalogue
    catalogueId = Objects.requireNonNull(Mono.from(databaseClient.sql(
        "INSERT INTO named.counter_catalogue (name, description, created, updated) " +
          "VALUES ('test-counter', 'Test Counter', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
      )
      .filter(statement -> statement.returnGeneratedValues("id"))
      .map(row -> row.get("id", Integer.class)).first()).block());
  }

  @Test
  void testCreateCounter() {
    // Test creating a counter
    StepVerifier.create(counterRepository.create(catalogueId, 1L, 10L))
      .assertNext(counter -> {
        assertNotNull(counter);
        assertEquals(catalogueId, counter.catalogue().id());
        assertEquals(1L, counter.userId());
        assertEquals(10L, counter.value());
        assertNotNull(counter.id());
        assertNotNull(counter.created());
        assertNotNull(counter.updated());
      })
      .verifyComplete();
  }

  @Test
  void testGetCounter() {
    // Create a counter first
    Mono.from(databaseClient.sql(
        "INSERT INTO named.counter_values (counter_id, user_id, \"value\", created, updated) " +
          "VALUES (:counterId, :userId, :value, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
      )
      .bind("counterId", catalogueId)
      .bind("userId", 1L)
      .bind("value", 10L)
      .then()).block();

    // Test getting the counter
    StepVerifier.create(counterRepository.get(catalogueId, 1L))
      .assertNext(counter -> {
        assertNotNull(counter);
        assertEquals(catalogueId, counter.catalogue().id());
        assertEquals(1L, counter.userId());
        assertEquals(10L, counter.value());
        assertNotNull(counter.id());
        assertNotNull(counter.created());
        assertNotNull(counter.updated());
      })
      .verifyComplete();
  }

  @Test
  void testGetNonExistentCounter() {
    // Test getting a counter that doesn't exist
    StepVerifier.create(counterRepository.get(catalogueId, 999L))
      .verifyComplete();
  }

  @Test
  void testUpdateCounter() {
    // Create a counter first
    Mono.from(databaseClient.sql(
        "INSERT INTO named.counter_values (counter_id, user_id, \"value\", created, updated) " +
          "VALUES (:counterId, :userId, :value, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
      )
      .bind("counterId", catalogueId)
      .bind("userId", 1L)
      .bind("value", 10L)
      .then()).block();

    // Test updating the counter
    StepVerifier.create(counterRepository.update(catalogueId, 1L, 20L))
      .assertNext(counter -> {
        assertNotNull(counter);
        assertEquals(catalogueId, counter.catalogue().id());
        assertEquals(1L, counter.userId());
        assertEquals(20L, counter.value());
        assertNotNull(counter.id());
        assertNotNull(counter.created());
        assertNotNull(counter.updated());
      })
      .verifyComplete();

    // Verify the update in the database
    StepVerifier.create(counterRepository.get(catalogueId, 1L))
      .assertNext(counter -> assertEquals(20L, counter.value()))
      .verifyComplete();
  }

  @Test
  void testUpdateNonExistentCounter() {
    // Test updating a counter that doesn't exist
    StepVerifier.create(counterRepository.update(catalogueId, 999L, 20L))
      .expectNextMatches(Objects::nonNull)
      .verifyComplete();
  }
}
