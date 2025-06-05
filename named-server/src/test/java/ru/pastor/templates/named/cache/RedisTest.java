package ru.pastor.templates.named.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisTest {

  private ReactiveRedisOperations<String, Integer> operations;
  private ReactiveValueOperations<String, Integer> valueOperations;
  private Redis redis;

  @BeforeEach
  void setUp() {
    operations = mock(ReactiveRedisOperations.class);
    valueOperations = mock(ReactiveValueOperations.class);

    when(operations.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(anyString())).thenReturn(Mono.just(10));
    when(valueOperations.set(anyString(), anyInt())).thenReturn(Mono.just(true));
    when(operations.delete(anyString())).thenReturn(Mono.just(1L));

    redis = new Redis(operations);
  }

  @Test
  void testGet() {
    // Test getting a value from Redis
    StepVerifier.create(redis.get("key1"))
      .expectNext(10)
      .verifyComplete();

    // Verify that the operations were called
    verify(operations).opsForValue();
    verify(valueOperations).get("key1");
  }

  @Test
  void testUpdate() {
    // Test updating a value in Redis
    StepVerifier.create(redis.update("key1", 20))
      .verifyComplete();

    // Verify that the operations were called
    verify(operations).opsForValue();
    verify(valueOperations).set("key1", 20);
  }

  @Test
  void testDelete() {
    // Test deleting a value from Redis
    StepVerifier.create(redis.delete("key1"))
      .verifyComplete();

    // Verify that the operations were called
    verify(operations).delete("key1");
  }

  @Test
  void testGetWithError() {
    // Setup mock to return an error
    when(valueOperations.get("error-key")).thenReturn(Mono.error(new RuntimeException("Test error")));

    // Test getting a value with an error - should return empty Mono due to error handling
    StepVerifier.create(redis.get("error-key"))
      .verifyComplete();

    // Verify that the operations were called
    verify(operations).opsForValue();
    verify(valueOperations).get("error-key");
  }

  @Test
  void testUpdateWithError() {
    // Setup mock to return an error
    when(valueOperations.set("error-key", 20)).thenReturn(Mono.error(new RuntimeException("Test error")));

    // Test updating a value with an error - should return empty Mono due to error handling
    StepVerifier.create(redis.update("error-key", 20))
      .verifyComplete();

    // Verify that the operations were called
    verify(operations).opsForValue();
    verify(valueOperations).set("error-key", 20);
  }

  @Test
  void testDeleteWithError() {
    // Setup mock to return an error
    when(operations.delete("error-key")).thenReturn(Mono.error(new RuntimeException("Test error")));

    // Test deleting a value with an error - should return empty Mono due to error handling
    StepVerifier.create(redis.delete("error-key"))
      .verifyComplete();

    // Verify that the operations were called
    verify(operations).delete("error-key");
  }
}
