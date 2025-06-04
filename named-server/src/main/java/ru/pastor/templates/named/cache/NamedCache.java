package ru.pastor.templates.named.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public interface NamedCache<K, V> {

  Mono<V> get(K key);

  Mono<Void> delete(K key);

  Mono<Void> update(K key, V value);

  interface All<K, V> {
    Mono<Void> all(BiFunction<K, V, Void> entry);
  }

  @SuppressWarnings("AbstractClassName")
  abstract class ReadOnly<K, V> implements NamedCache<K, V> {

    @Override
    public final Mono<Void> delete(K key) {
      return Mono.empty();
    }

    @Override
    public final Mono<Void> update(K key, V value) {
      return Mono.empty();
    }
  }

  final class Local<K, V> implements NamedCache<K, V> {
    private final Cache<K, V> cache;
    private final NamedCache<K, V> delegate;

    public Local(String name,
                 Duration expires,
                 MeterRegistry registry,
                 NamedCache<K, V> delegate,
                 All<K, V> all) {
      this.cache = CacheBuilder.newBuilder()
        .recordStats()
        .weakValues()
        .expireAfterWrite(expires.toSeconds(), TimeUnit.SECONDS)
        .build();
      this.delegate = delegate;
      Gauge.builder("Cache_" + name + "_hit_count", () -> cache.stats().hitCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_miss_count", () -> cache.stats().missCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_eviction_count", () -> cache.stats().evictionCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_total_load_time", () -> cache.stats().totalLoadTime())
        .register(registry);
      if (all != null) {
        all.all((k, v) -> {
          cache.put(k, v);
          return null;
        }).subscribe();
      }
    }

    @Override
    public Mono<V> get(K key) {
      return CacheMono.lookup(k -> Mono.justOrEmpty(cache.getIfPresent(key)).map(Signal::next), key)
        .onCacheMissResume(() -> delegate.get(key).cache())
        .andWriteWith((k, signal) -> Mono.fromRunnable(() -> Optional.ofNullable(signal.get())
          .ifPresent(value -> cache.put(key, value))));
    }

    @Override
    public Mono<Void> delete(K key) {
      return delegate.delete(key).then(Mono.fromRunnable(() -> cache.invalidate(key)));
    }

    @Override
    public Mono<Void> update(K key, V value) {
      return delegate.update(key, value)
        .then(Mono.fromRunnable(() -> cache.put(key, value)));
    }
  }
}
