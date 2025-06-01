package ru.pastor.templates.named.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public final class Redis implements NamedCache<String, Long> {
  private final ReactiveRedisOperations<String, Long> operations;

  @Override
  public Mono<Long> get(String key) {
    return operations.opsForValue().get(key);
  }

  @Override
  public Mono<Void> delete(String key) {
    return operations.opsForValue().delete(key).then();
  }

  @Override
  public Mono<Void> update(String key, Long value) {
    return operations.opsForValue().set(key, value).then();
  }
}
