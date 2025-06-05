package ru.pastor.templates.named.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisTest {

  private ReactiveRedisOperations<String, Number> operations;
  private ReactiveValueOperations<String, Number> valueOperations;
  private Redis redis;

  @BeforeEach
  void setUp() {
    Map<String, Integer> values = new HashMap<>();
    operations = mock(ReactiveRedisOperations.class);
    valueOperations = mock(ReactiveValueOperations.class);

    when(operations.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(anyString())).thenReturn(Mono.just(10));
    when(valueOperations.set(anyString(), anyInt())).thenReturn(Mono.just(true));
    when(operations.delete(anyString())).thenReturn(Mono.just(1L));
    when(operations.opsForValue().increment(anyString(), anyLong()))
      .thenAnswer(args -> Mono.just(values.getOrDefault(
        (String) args.getArgument(0), 0) +
        ((Number) args.getArgument(1)).longValue()));
    when(operations.opsForValue().get(anyString())).thenAnswer(new Answer<Mono<Integer>>() {

      @Override
      public Mono<Integer> answer(InvocationOnMock args) throws Throwable {
        return Mono.just(values.getOrDefault((String) args.getArgument(0), 0));
      }
    });

    redis = new Redis(operations);
  }

  @Test
  void testGet() {
    // Test getting a value from Redis
    StepVerifier.create(redis.get("key1"))
      .expectNext(0)
      .verifyComplete();

    // Verify that the operations were called
    verify(operations, times(3)).opsForValue();
    verify(valueOperations).get("key1");
  }

  @Test
  void testIncrement() {
    // Test updating a value in Redis
    StepVerifier.create(redis.increment("key1", 20))
      .expectNext(20)
      .verifyComplete();

    // Verify that the operations were called
    verify(operations, times(3)).opsForValue();
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
    verify(operations, times(3)).opsForValue();
  }

  @Test
  void testIncrementWithError() {
    // Setup mock to return an error
    when(valueOperations.set("error-key", 20)).thenReturn(Mono.error(new RuntimeException("Test error")));

    // Test updating a value with an error - should return empty Mono due to error handling
    StepVerifier.create(redis.increment("error-key", 20))
      .expectNext(20)
      .verifyComplete();

    // Verify that the operations were called
    verify(operations, times(3)).opsForValue();
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
