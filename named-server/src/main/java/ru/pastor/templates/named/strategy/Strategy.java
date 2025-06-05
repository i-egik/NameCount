package ru.pastor.templates.named.strategy;

import reactor.core.publisher.Mono;

public interface Strategy {
  interface ReactiveCounter {
    Mono<Long> get(long userId, String counterName);

    Mono<Void> set(long userId, String counterName, long value);

    Mono<Void> delete(long userId, String counterName);

    Mono<Long> increment(long userId, String counterName);

    Mono<Long> increment(long userId, String counterName, long delta);

    Mono<Long> decrement(long userId, String counterName);

    Mono<Void> reset(long userId, String counterName);
  }
}
