package ru.pastor.templates.named.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.cache.NamedCache;

public interface NamedCountService {

  Mono<Long> get(String name, long userId);

  Mono<Long> increment(String name, long userId);

  Mono<Long> increment(String name, long userId, long delta);

  @Slf4j
  @RequiredArgsConstructor
  @Service("NamedCountService.Standard")
  class Standard implements NamedCountService {
    private final NamedCache<String, Long> cache;

    private static String key(String name, long userId) {
      return String.format("named:%s:%d", name, userId);
    }

    @Override
    public Mono<Long> get(String name, long userId) {
      return cache.get(key(name, userId));
    }

    @Override
    public Mono<Long> increment(String name, long userId) {
      return increment(name, userId, 1);
    }

    @Override
    public Mono<Long> increment(String name, long userId, long delta) {
      String key = key(name, userId);
      return cache.get(key)
        .flatMap(value -> cache.update(key, value + delta).thenReturn(value + delta));
    }
  }
}
