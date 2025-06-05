package ru.pastor.templates.named.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.pastor.templates.named.server.grpc.CatalogueFilter;
import ru.pastor.templates.named.server.grpc.CataloguePutRequest;

class NamedServerCatalogueServiceGrpcTest extends BasisTestSuit {

  private NamedServerCatalogueServiceGrpc grpcService;

  @Autowired
  private DatabaseClient databaseClient;

  @Autowired
  private NamedCatalogueService namedCatalogueService;

  @BeforeEach
  protected void setUp() {
    super.setUp();
    grpcService = new NamedServerCatalogueServiceGrpc(namedCatalogueService);

    if (databaseClient != null) {
      try {
        // Insert test data into counter_catalogue
        Mono.from(databaseClient.sql(
          "INSERT INTO counter_catalogue (name, description, created, updated) " +
            "VALUES ('test-catalogue', 'Test Catalogue', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        ).then()).block();
      } catch (Exception e) {
        System.out.println("[DEBUG_LOG] Error inserting test data: " + e.getMessage());
      }
    } else {
      System.out.println("[DEBUG_LOG] DatabaseClient is null in NamedServerCatalogueServiceGrpcTest.setUp");
    }
  }

  @Test
  void testList() {
    CatalogueFilter request = CatalogueFilter.newBuilder()
      .setName("test-catalogue")
      .build();
    StepVerifier.create(grpcService.list(request))
      .expectNextCount(1)
      .verifyComplete();
  }

  @Test
  void testPut() {
    CataloguePutRequest request = CataloguePutRequest.newBuilder()
      .setName("test-catalogue2")
      .build();

    StepVerifier.create(grpcService.put(request))
      .expectNextCount(1)
      .verifyComplete();
  }
}
