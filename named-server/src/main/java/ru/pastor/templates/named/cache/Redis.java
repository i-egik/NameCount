package ru.pastor.templates.named.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Реализация интерфейса NamedCache для работы с Redis.
 * Предоставляет реактивный доступ к операциям с кэшем Redis для хранения пар ключ-значение.
 * Обрабатывает ошибки Redis и логирует их, возвращая пустые Mono в случае сбоев.
 */
@Slf4j
@RequiredArgsConstructor
public final class Redis implements NamedCache<String, Integer> {
  /**
   * Операции Redis для реактивной работы с парами ключ-значение.
   * Используется для выполнения базовых операций get, set и delete.
   */
  private final ReactiveRedisOperations<String, Number> operations;
  private final long ttlSeconds;

  private static void onError(String key, Integer value, Throwable e) {
    log.error("Error updating key {} with value {}: {}", key, value, e.getMessage());
  }

  /**
   * {@inheritDoc}
   * Получает значение из Redis по ключу.
   * В случае ошибки логирует сообщение и возвращает пустой Mono.
   */
  @Override
  public Mono<Integer> get(String key) {
    return operations.opsForValue().get(key)
      .map(Number::intValue)
      .onErrorResume(e -> {
        log.error("Error getting value for key {}: {}", key, e.getMessage());
        return Mono.empty();
      });
  }

  /**
   * {@inheritDoc}
   * Удаляет значение из Redis по ключу.
   * В случае ошибки логирует сообщение и возвращает пустой Mono.
   */
  @Override
  public Mono<Void> delete(String key) {
    return operations.delete(key)
      .onErrorResume(e -> {
        log.error("Error deleting key {}: {}", key, e.getMessage());
        return Mono.just(0L);
      })
      .then();
  }

  /**
   * {@inheritDoc}
   * Обновляет значение в Redis по ключу.
   * В случае ошибки логирует сообщение и возвращает пустой Mono.
   */
  @Override
  public Mono<Integer> increment(String key, Integer value) {
    return operations.opsForValue().increment(key, value.longValue())
      .map(Long::intValue)
      .doOnError(e -> onError(key, value, e));
  }

  @Override
  public Mono<Integer> update(String key, Integer value) {
    return operations.opsForValue().set(key, value)
      .flatMap(v -> Mono.just(value))
      .doOnError(e -> onError(key, value, e));
  }
}
