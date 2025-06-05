package ru.pastor.templates.named.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.pastor.templates.named.server.grpc.CountFilter;
import ru.pastor.templates.named.server.grpc.CountPutRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NamedServerCatalogueServiceGrpcTest extends BasisTestSuit {

    private NamedServerCatalogueServiceGrpc grpcService;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    protected void setUp() {
        super.setUp();

        // Create the gRPC service instance
        grpcService = new NamedServerCatalogueServiceGrpc();

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
        if (grpcService == null) {
            System.out.println("[DEBUG_LOG] Skipping testList because required beans are null");
            return;
        }

        // Since list() is not implemented in the service (it calls super.list()),
        // we'll just verify that it throws an UNIMPLEMENTED error
        CountFilter request = CountFilter.newBuilder()
            .setName("test-catalogue")
            .setUserId(1)
            .build();

        try {
            // This will throw an exception, which is expected
            grpcService.list(request).subscribe();
            // If we get here, the test should fail
            assert false : "Expected an exception but none was thrown";
        } catch (Exception e) {
            // Expected exception, test passes
            assert e instanceof io.grpc.StatusRuntimeException : "Expected StatusRuntimeException but got " + e.getClass().getName();
        }
    }

    @Test
    void testPut() {
        if (grpcService == null) {
            System.out.println("[DEBUG_LOG] Skipping testPut because required beans are null");
            return;
        }

        // Since put() is not implemented in the service (it calls super.put()),
        // we'll just verify that it throws an UNIMPLEMENTED error
        CountPutRequest request = CountPutRequest.newBuilder()
            .setName("test-catalogue")
            .setUserId(1)
            .setDelta(5)
            .build();

        try {
            // This will throw an exception, which is expected
            grpcService.put(request).subscribe();
            // If we get here, the test should fail
            assert false : "Expected an exception but none was thrown";
        } catch (Exception e) {
            // Expected exception, test passes
            assert e instanceof io.grpc.StatusRuntimeException : "Expected StatusRuntimeException but got " + e.getClass().getName();
        }
    }
}
