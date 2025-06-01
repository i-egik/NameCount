package ru.pastor.templates.named.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.cache.NamedCache;

public interface NamedCountService {

  Mono<Long> get(String name, long userId);

  Mono<Long> increment(String name, long userId);

  Mono<Long> increment(String name, long userId, long delta);

  @Slf4j
  @RequiredArgsConstructor
  final class Standard implements NamedCountService {
    private final NamedCache<String, Long> values;
    private final NamedCache<String, Long> catalogue;

    private Mono<String> key(String name, long userId) {
      return catalogue.get(name).map(v -> String.format("named:%d:%d", userId, v));
    }

    @Override
    public Mono<Long> get(String name, long userId) {
      return key(name, userId).flatMap(values::get);
    }

    @Override
    public Mono<Long> increment(String name, long userId) {
      return increment(name, userId, 1);
    }

    @Override
    public Mono<Long> increment(String name, long userId, long delta) {
      return key(name, userId).flatMap(k -> values.get(k)
        .flatMap(value -> values.update(k, value + delta).thenReturn(value + delta)));
    }
  }
}
