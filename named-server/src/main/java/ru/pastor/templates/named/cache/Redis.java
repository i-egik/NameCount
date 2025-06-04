package ru.pastor.templates.named.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public final class Redis implements NamedCache<String, Integer> {
  private final ReactiveRedisOperations<String, Integer> operations;

  @Override
  public Mono<Integer> get(String key) {
    return operations.opsForValue().get(key)
      .onErrorResume(e -> {
        log.error("Error getting value for key {}: {}", key, e.getMessage());
        return Mono.empty();
      });
  }

  @Override
  public Mono<Void> delete(String key) {
    return operations.opsForValue().delete(key)
      .onErrorResume(e -> {
        log.error("Error deleting key {}: {}", key, e.getMessage());
        return Mono.just(false);
      })
      .then();
  }

  @Override
  public Mono<Void> update(String key, Integer value) {
    return operations.opsForValue().set(key, value)
      .onErrorResume(e -> {
        log.error("Error updating key {} with value {}: {}", key, value, e.getMessage());
        return Mono.just(false);
      })
      .then();
  }
}
