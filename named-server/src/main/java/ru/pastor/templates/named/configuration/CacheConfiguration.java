package ru.pastor.templates.named.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.cache.NamedCache;
import ru.pastor.templates.named.cache.Redis;
import ru.pastor.templates.named.repository.CatalogueRepository;
import ru.pastor.templates.named.repository.entity.CatalogueEntity;

import java.time.Duration;

@Slf4j
@Configuration
public class CacheConfiguration {

  @Bean("NamedCache.Redis")
  public NamedCache<String, Long> redisNamedCache(ReactiveRedisOperations<String, Long> operations) {
    return new Redis(operations);
  }

  @Bean("NamedCache.Values")
  public NamedCache<String, Long> valuesNamedCache(MeterRegistry registry,
                                                   @Qualifier("NamedCache.Redis") NamedCache<String, Long> redisCache) {
    return new NamedCache.Local<>("values", Duration.ofHours(1), registry, redisCache, null);
  }

  @Bean("NamedCache.Catalogue")
  public NamedCache<String, Long> catalogueNamedCache(MeterRegistry registry,
                                                      CatalogueRepository repository) {
    return new NamedCache.Local<>("catalogue", Duration.ofHours(4), registry, new NamedCache.ReadOnly<>() {
      @Override
      public Mono<Long> get(String key) {
        return repository.get(key).map(CatalogueEntity::id);
      }
    }, entry ->
      repository.counters(new CatalogueRepository.Filter())
        .map(e -> {
          entry.apply(e.name(), e.id());
          return e;
        })
        .then());
  }
}
