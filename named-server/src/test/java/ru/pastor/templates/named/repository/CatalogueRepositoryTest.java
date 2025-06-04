package ru.pastor.templates.named.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.pastor.templates.named.service.BasisTestSuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CatalogueRepositoryTest extends BasisTestSuit {

  @Autowired
  @Qualifier("CatalogueRepository.Postgres")
  private CatalogueRepository catalogueRepository;

  @Autowired
  private DatabaseClient databaseClient;

  @BeforeEach
  protected void setUp() {
    super.setUp();
    // Clear existing data
    Mono.from(databaseClient.sql("DELETE FROM counter_catalogue").then()).block();

    // Insert test data
    Mono.from(databaseClient.sql(
      "INSERT INTO counter_catalogue (name, description, created, updated) " +
        "VALUES ('test-counter', 'Test Counter', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    ).then()).block();
  }

  @Test
  void testGetByName() {
    // Test getting a catalogue by name
    StepVerifier.create(catalogueRepository.get("test-counter"))
      .assertNext(catalogue -> {
        assertNotNull(catalogue);
        assertEquals("test-counter", catalogue.name());
        assertEquals("Test Counter", catalogue.description());
        assertNotNull(catalogue.id());
        assertNotNull(catalogue.created());
        assertNotNull(catalogue.updated());
      })
      .verifyComplete();
  }

  @Test
  void testGetNonExistentCatalogue() {
    // Test getting a catalogue that doesn't exist
    StepVerifier.create(catalogueRepository.get("non-existent"))
      .verifyComplete();
  }

  @Test
  void testCounters() {
    // Test getting all catalogues
    StepVerifier.create(catalogueRepository.counters(new CatalogueRepository.Filter()))
      .assertNext(catalogue -> {
        assertNotNull(catalogue);
        assertEquals("test-counter", catalogue.name());
        assertEquals("Test Counter", catalogue.description());
        assertNotNull(catalogue.id());
        assertNotNull(catalogue.created());
        assertNotNull(catalogue.updated());
      })
      .verifyComplete();
  }

  @Test
  void testCountersWithMultipleEntries() {
    // Insert another test entry
    Mono.from(databaseClient.sql(
      "INSERT INTO counter_catalogue (name, description, created, updated) " +
        "VALUES ('another-counter', 'Another Test Counter', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    ).then()).block();

    // Test getting all catalogues
    StepVerifier.create(catalogueRepository.counters(new CatalogueRepository.Filter()).count())
      .expectNext(2L)
      .verifyComplete();
  }
}
