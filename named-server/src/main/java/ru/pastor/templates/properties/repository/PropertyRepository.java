package ru.pastor.templates.properties.repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.cache.CacheMono;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import ru.pastor.templates.properties.Attribute;
import ru.pastor.templates.properties.Property;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface PropertyRepository {

  Flux<Property> listProperties(Page page);

  Mono<Void> createOrUpdate(Property property);

  Mono<Property> fetch(String key);

  Mono<Void> delete(String key);

  record Page(int offset, int limit) {
    public static Page of(int offset, int limit) {
      return new Page(offset, limit);
    }

    public static Page of(int offset) {
      return new Page(offset, 100);
    }

    public static Page of() {
      return of(0);
    }
  }

  @Slf4j
  class Cached implements PropertyRepository {
    private final PropertyRepository delegate;
    private final Cache<String, Property> cache;

    public Cached(String name, Duration expires, MeterRegistry registry, PropertyRepository delegate) {
      this.delegate = delegate;
      this.cache = CacheBuilder.newBuilder()
        .recordStats()
        .weakValues()
        .expireAfterWrite(expires.toSeconds(), TimeUnit.SECONDS)
        .build();
      Gauge.builder("Cache_" + name + "_hit_count", () -> cache.stats().hitCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_miss_count", () -> cache.stats().missCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_eviction_count", () -> cache.stats().evictionCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_total_load_time", () -> cache.stats().totalLoadTime())
        .register(registry);
    }

    @Override
    public Flux<Property> listProperties(Page page) {
      return delegate.listProperties(page);
    }

    @Override
    public Mono<Void> createOrUpdate(Property property) {
      return delegate.createOrUpdate(property)
        .then(Mono.fromRunnable(() -> {
          cache.put(property.key(), property);
        }));
    }

    @Override
    public Mono<Property> fetch(String key) {
      return CacheMono.lookup(k -> Mono.justOrEmpty(cache.getIfPresent(key)).map(Signal::next), key)
        .onCacheMissResume(() -> delegate.fetch(key))
        .andWriteWith((k, signal) -> Mono.fromRunnable(() -> Optional.ofNullable(signal.get())
          .ifPresent(value -> cache.put(key, value))));
    }

    @Override
    public Mono<Void> delete(String key) {
      return delegate.delete(key).then(Mono.fromRunnable(() -> cache.invalidate(key)));
    }
  }

  @Slf4j
  class Standard implements PropertyRepository {
    private final DatabaseClient client;
    private final TransactionalOperator tx;
    private final BiFunction<Row, RowMetadata, Property> mapper;
    private final Function<Page, String> listProperties;
    private final Function<Property, String> createOrUpdate;
    private final Function<String, String> fetch;
    private final Function<String, String> delete;

    public Standard(DatabaseClient client,
                    TransactionalOperator tx,
                    BiFunction<Row, RowMetadata, Property> mapper,
                    Function<Page, String> listProperties,
                    Function<Property, String> createOrUpdate,
                    Function<String, String> fetch,
                    Function<String, String> delete) {
      this.client = client;
      this.tx = tx;
      this.mapper = mapper;
      this.listProperties = listProperties;
      this.createOrUpdate = createOrUpdate;
      this.fetch = fetch;
      this.delete = delete;
    }

    @Override
    public Flux<Property> listProperties(Page page) {
      return client.sql(listProperties.apply(page))
        .bind("offset", page.offset)
        .bind("limit", page.limit)
        .map(mapper)
        .all()
        .as(tx::transactional);
    }

    @Override
    public Mono<Void> createOrUpdate(Property property) {
      DatabaseClient.GenericExecuteSpec spec = client.sql(createOrUpdate.apply(property))
        .bind("key", property.key())
        .bind("value", property.asString());
      Object expires = property.attribute(Attribute.Default.EXPIRES).orElse(null);
      if (expires != null) {
        spec = spec.bind("expires", expires);
      } else {
        spec = spec.bindNull("expires", LocalDateTime.class);
      }
      return spec
        .bind("refresh", property.attribute(Attribute.Default.REFRESH).orElse(3200))
        .fetch().rowsUpdated()
        .as(tx::transactional)
        .doOnError(err -> log.error("createOrUpdate failed", err))
        .then();
    }

    @Override
    public Mono<Property> fetch(String key) {
      return client.sql(fetch.apply(key))
        .bind("key", key)
        .map(mapper)
        .one()
        .as(tx::transactional);
    }

    @Override
    public Mono<Void> delete(String key) {
      return client.sql(delete.apply(key))
        .bind("key", key)
        .fetch()
        .rowsUpdated()
        .as(tx::transactional)
        .then();
    }
  }
}
