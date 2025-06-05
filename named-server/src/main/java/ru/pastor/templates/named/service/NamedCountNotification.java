package ru.pastor.templates.named.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.pastor.templates.named.repository.CounterRepository;

import java.time.Duration;
import java.util.Map;

public interface NamedCountNotification {
  Mono<Void> update(long userId, long counterId, long value);

  @Slf4j
  @RequiredArgsConstructor
  @Service("NamedStream")
  class Default implements NamedCountNotification {
    private static final String STREAM_KEY = "counter_updates";
    private final ReactiveRedisTemplate<String, String> template;
    private final CounterRepository repository;

    @EventListener(ApplicationReadyEvent.class)
    private void ready() {
      readCounterUpdates()
        .flatMap(changes -> {
          Long userId = (Long) changes.get("user-id");
          Long counterId = (Long) changes.get("counter-id");
          Long value = (Long) changes.get("value");
          if (counterId != null && userId != null && value != null) {
            return repository.update(counterId, userId, value).then();
          } else {
            log.error("counter-name or counter-id is null. changes: {}", changes);
            return Mono.empty();
          }
        })
        .subscribe();
    }

    @Override
    public Mono<Void> update(long userId, long counterId, long value) {
      MapRecord<String, String, Object> record = StreamRecords.<String, String, Object>mapBacked(Map.of(
          "user-id", userId,
          "counter-id", counterId,
          "value", value
        ))
        .withStreamKey(STREAM_KEY);

      return template.opsForStream().add(record).then();
    }

    public Flux<Map<Object, Object>> readCounterUpdates() {
      return template.opsForStream()
        .read(StreamOffset.fromStart(STREAM_KEY))
        .map(Record::getValue);
    }

    // Чтение с обработкой групп потребителей
    @SuppressWarnings("unchecked")
    public Flux<Map<Object, Object>> consumerGroupRead(String groupName, String consumerId) {
      return template.opsForStream()
        .read(Consumer.from(groupName, consumerId),
          StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
        )
        .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
        .map(Record::getValue);
    }
  }
}
