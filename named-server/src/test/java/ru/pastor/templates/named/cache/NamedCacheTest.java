package ru.pastor.templates.named.cache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NamedCacheTest {

  private NamedCache<String, Long> delegateCache;
  private NamedCache<String, Long> localCache;
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    delegateCache = mock(NamedCache.class);
    when(delegateCache.get(anyString())).thenReturn(Mono.just(10L));
    when(delegateCache.increment(anyString(), any())).thenReturn(Mono.empty());
    when(delegateCache.delete(anyString())).thenReturn(Mono.empty());

    localCache = new NamedCache.Local<>("test", Duration.ofMinutes(10), meterRegistry, delegateCache, null);
  }

  @Test
  void testGet() {
    // First call should delegate to the underlying cache
    StepVerifier.create(localCache.get("key1"))
      .expectNext(10L)
      .verifyComplete();

    // Second call should use the cached value
    StepVerifier.create(localCache.get("key1"))
      .expectNext(10L)
      .verifyComplete();

    // Verify that the delegate was called only once
    verify(delegateCache, times(1)).get("key1");
  }

  @Test
  void testIncrement() {
    // Update should update both the local cache and the delegate
    StepVerifier.create(localCache.increment("key1", 20L))
      .verifyComplete();

    // Get should now return the updated value without calling the delegate
    StepVerifier.create(localCache.get("key1"))
      .expectNext(10L)
      .verifyComplete();

    // Verify that the delegate was called for the update
    verify(delegateCache).increment("key1", 20L);
  }

  @Test
  void testDelete() {
    // First get to cache the value
    StepVerifier.create(localCache.get("key1"))
      .expectNext(10L)
      .verifyComplete();

    // Delete should invalidate the cache and call the delegate
    StepVerifier.create(localCache.delete("key1"))
      .verifyComplete();

    // Next get should call the delegate again
    StepVerifier.create(localCache.get("key1"))
      .expectNext(10L)
      .verifyComplete();

    // Verify that the delegate was called for both gets and the delete
    verify(delegateCache, times(2)).get("key1");
    verify(delegateCache).delete("key1");
  }

  @Test
  void testReadOnlyCache() {
    // Create a read-only cache
    NamedCache<String, Long> readOnlyCache = new NamedCache.ReadOnly<>() {
      @Override
      public Mono<Long> get(String key) {
        return Mono.just(5L);
      }
    };

    // Get should work
    StepVerifier.create(readOnlyCache.get("key1"))
      .expectNext(5L)
      .verifyComplete();

    // Update and delete should return empty
    StepVerifier.create(readOnlyCache.increment("key1", 20L))
      .expectNext(20L)
      .verifyComplete();

    StepVerifier.create(readOnlyCache.delete("key1"))
      .verifyComplete();
  }
}
