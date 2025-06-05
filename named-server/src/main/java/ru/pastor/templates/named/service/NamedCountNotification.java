package ru.pastor.templates.named.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
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
    private static final String STREAM_GROUP_KEY = "counter-updates-group";
    private static final String STREAM_CONSUMER_KEY = "counter-updates-consumer-id";
    private final ReactiveRedisTemplate<String, Number> template;
    private final CounterRepository repository;

    @EventListener(ApplicationReadyEvent.class)
    private void ready() {
      template.opsForStream().createGroup(STREAM_KEY, STREAM_GROUP_KEY).subscribe();
      streamMessages(STREAM_GROUP_KEY, STREAM_CONSUMER_KEY)
        .flatMap(message -> {
          var changes = message.getValue();
          Number userId = (Number) changes.get("user-id");
          Number counterId = (Number) changes.get("counter-id");
          Number value = (Number) changes.get("value");
          if (counterId != null && userId != null && value != null) {
            return repository.update(counterId.longValue(), userId.longValue(), value.longValue()).then();
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

    @SuppressWarnings("unchecked")
    private Flux<MapRecord<String, Object, Object>> streamMessages(String groupName, String consumerId) {
      return template.opsForStream()
        .read(Consumer.from(groupName, consumerId),
          StreamReadOptions.empty()
            .count(100)
            .block(Duration.ofSeconds(2)),
          StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()))
        .repeat()
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
          .maxAttempts(5))
        .onErrorContinue((err, obj) ->
          log.error("Ошибка чтения стрима: {}", err.getMessage()));
    }

  }
}
