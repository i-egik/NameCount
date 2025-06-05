package ru.pastor.templates.named.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.cache.NamedCache;
import ru.pastor.templates.named.repository.CatalogueRepository;
import ru.pastor.templates.named.repository.CounterRepository;
import ru.pastor.templates.named.service.NamedCatalogueService;
import ru.pastor.templates.named.service.NamedCountNotification;
import ru.pastor.templates.named.service.NamedCountService;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Import({
  R2dbcAutoConfiguration.class,
  DatabaseConfiguration.class,
  CounterRepository.Postgres.class,
  CatalogueRepository.Postgres.class
})
public class TestConfiguration {

  @Bean
  public NamedCountNotification notification() {
    return (userId, counterId, value) -> Mono.empty();
  }

  @Bean
  public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
    return new R2dbcEntityTemplate(connectionFactory);
  }

  @Bean
  @Primary
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }

  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public ReactiveRedisOperations<String, Integer> mockRedisOperations() {
    return mock(ReactiveRedisOperations.class);
  }

  @Bean("NamedCache.Redis")
  @Primary
  @SuppressWarnings("unchecked")
  public NamedCache<String, Integer> mockRedisCache() {
    NamedCache<String, Integer> mock = mock(NamedCache.class);
    when(mock.get(anyString())).thenReturn(Mono.just(1));
    when(mock.increment(anyString(), anyInt()))
      .thenAnswer(i -> Mono.just(((int)i.getArgument(1)) + 1));
    when(mock.delete(anyString())).thenReturn(Mono.empty());
    return mock;
  }

  @Bean("NamedCache.Values")
  public NamedCache<String, Integer> valuesNamedCache(MeterRegistry registry,
                                                      @Qualifier("NamedCache.Redis") NamedCache<String, Integer> redisCache) {
    return new NamedCache.Local<>("values", Duration.ofHours(1), registry, redisCache, null);
  }

  @Bean("NamedCache.Catalogue")
  @SuppressWarnings("unchecked")
  public NamedCache<String, Integer> mockCatalogueCache() {
    NamedCache<String, Integer> mock = mock(NamedCache.class);
    when(mock.get(anyString())).thenReturn(Mono.just(1));
    when(mock.increment(anyString(), any())).thenReturn(Mono.empty());
    when(mock.delete(anyString())).thenReturn(Mono.empty());
    return mock;
  }

  @Bean
  public NamedCountService namedCountService(
    @Qualifier("NamedCache.Values") NamedCache<String, Integer> values,
    @Qualifier("NamedCache.Catalogue") NamedCache<String, Integer> catalogue,
    NamedCountNotification notification) {
    return new NamedCountService.Standard(values, catalogue, notification);
  }

  @Bean
  public NamedCatalogueService namedCatalogueService(
    @Qualifier("NamedCache.Catalogue") NamedCache<String, Integer> catalogue,
    CatalogueRepository catalogueRepository) {
    return new NamedCatalogueService.Standard(catalogue, catalogueRepository);
  }
}
