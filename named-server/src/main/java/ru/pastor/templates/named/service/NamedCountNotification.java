package ru.pastor.templates.named.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.pastor.templates.named.configuration.RedisConfiguration;
import ru.pastor.templates.named.repository.CounterRepository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface NamedCountNotification {
  Mono<Void> update(long userId, long counterId, long value);

  @Slf4j
  @Service("NamedStream")
  class NamedStream implements NamedCountNotification {
    private static final String STREAM_KEY = "counter_updates";
    private static final String STREAM_GROUP_KEY = "counter-updates-group";
    private static final String STREAM_CONSUMER_KEY = "counter-updates-consumer-id";
    private final ReactiveRedisTemplate<String, ?> template;
    private final CounterRepository repository;
    private final ChangeValue.Strategy strategy;

    public NamedStream(ReactiveRedisConnectionFactory factory,
                       CounterRepository repository,
                       @Value("${app.stream.strategy:NUMBER}") String strategy) {
      this.strategy = ChangeValue.ChangeStrategy.of(strategy.toUpperCase());
      this.template = this.strategy.redisOperations(factory);
      this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void ready() {
      template.opsForStream().createGroup(STREAM_KEY, STREAM_GROUP_KEY).subscribe();
      streamMessages(STREAM_GROUP_KEY, STREAM_CONSUMER_KEY)
        .flatMap(message -> {
          Optional<ChangeValue> read = strategy.read(message);
          if (read.isPresent()) {
            var changeValue = read.get();
            if (log.isDebugEnabled()) {
              log.debug("Change value for {}: {}", STREAM_KEY, changeValue);
            }
            return repository.update(changeValue.counterId, changeValue.userId, changeValue.value).then();
          }
          log.error("Message can't parsed: {}", message);
          return Mono.empty();
        })
        .subscribe();
    }

    @Override
    public Mono<Void> update(long userId, long counterId, long value) {
      Record<String, ?> record = strategy.create(userId, counterId, value);
      return template.opsForStream().add(record)
        .doOnError(throwable -> log.error("Error while updating counter values", throwable))
        .then();
    }

    @SuppressWarnings("unchecked")
    private Flux<MapRecord<String, Object, Object>> streamMessages(String groupName, String consumerId) {
      return template.opsForStream()
        .read(Consumer.from(groupName, consumerId),
          StreamReadOptions.empty()
            .count(100)
            .block(Duration.ofSeconds(30)),
          StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()))
        .repeat()
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
          .maxAttempts(5))
        .onErrorContinue((err, obj) ->
          log.error("Ошибка чтения стрима: {}", err.getMessage()));
    }

    record ChangeValue(long userId, long counterId, long value) {
      @SuppressWarnings("unchecked")
      enum ChangeStrategy implements Strategy {
        NUMBER {
          @Override
          public Record<String, ?> create(long userId, long counterId, long value) {
            return StreamRecords.<String, String, Object>mapBacked(Map.of(
                "user-id", userId,
                "counter-id", counterId,
                "value", value
              ))
              .withStreamKey(STREAM_KEY);
          }

          @Override
          public Optional<ChangeValue> read(Record<String, ?> record) {
            Map<String, Object> changes = (Map<String, Object>) record.getValue();
            Number userId = (Number) changes.get("user-id");
            Number counterId = (Number) changes.get("counter-id");
            Number value = (Number) changes.get("value");
            if (counterId != null && userId != null && value != null) {
              return Optional.of(new ChangeValue(counterId.longValue(), userId.longValue(), value.longValue()));
            } else {
              return Optional.empty();
            }
          }

          @Override
          public ReactiveRedisTemplate<String, ?> redisOperations(ReactiveRedisConnectionFactory factory) {
            return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.<String, Number>newSerializationContext()
              .key(RedisSerializer.string())
              .value(RedisSerializationContext.SerializationPair.fromSerializer(RedisConfiguration.INSTANCE))
              .hashKey(RedisSerializer.string())
              .hashValue(RedisSerializationContext.SerializationPair.fromSerializer(RedisConfiguration.INSTANCE))
              .build());
          }
        },
        STRING {
          @Override
          public Record<String, ?> create(long userId, long counterId, long value) {
            return StreamRecords.<String, String, String>mapBacked(Map.of(
                "user-id", String.valueOf(userId),
                "counter-id", String.valueOf(counterId),
                "value", String.valueOf(value)
              ))
              .withStreamKey(STREAM_KEY);
          }

          @Override
          public Optional<ChangeValue> read(Record<String, ?> record) {
            Map<String, String> changes = (Map<String, String>) record.getValue();
            try {
              long userId = Long.parseLong(changes.get("user-id"));
              long counterId = Long.parseLong(changes.get("counter-id"));
              long value = Long.parseLong(changes.get("value"));
              return Optional.of(new ChangeValue(counterId, userId, value));
            } catch (Exception ex) {
              log.error("Can't fetch values on {}. Error: {}", changes, ex.getMessage());
              return Optional.empty();
            }
          }

          @Override
          public ReactiveRedisTemplate<String, ?> redisOperations(ReactiveRedisConnectionFactory factory) {
            return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.string());
          }
        },
        SCALAR {
          @Override
          public Record<String, ?> create(long userId, long counterId, long value) {
            return StreamRecords.newRecord()
              .ofObject(String.format("%d:%d:%d", userId, counterId, value))
              .withStreamKey(STREAM_KEY);
          }

          @Override
          public Optional<ChangeValue> read(Record<String, ?> record) {
            String changes = ((Map<String, Object>) record.getValue()).get("payload").toString();
            String[] parts = changes.split(":");
            if (parts.length == 3) {
              try {
                long userId = Long.parseLong(parts[0]);
                long counterId = Long.parseLong(parts[1]);
                long value = Long.parseLong(parts[2]);
                return Optional.of(new ChangeValue(userId, counterId, value));
              } catch (NumberFormatException e) {
                log.error("Error parsing counter-name:counter-id:value: {}", changes, e);
              }
            } else {
              log.error("Error parsing counter-name:counter-id:value: {}", changes);
            }
            return Optional.empty();
          }

          @Override
          public ReactiveRedisTemplate<String, ?> redisOperations(ReactiveRedisConnectionFactory factory) {
            return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.string());
          }
        };

        private static ChangeStrategy of(String strategy) {
          for (ChangeStrategy s : values()) {
            if (s.name().equalsIgnoreCase(strategy)) {
              return s;
            }
          }
          throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }

        public abstract Record<String, ?> create(long userId, long counterId, long value);

        public abstract Optional<ChangeValue> read(Record<String, ?> record);

        public abstract ReactiveRedisTemplate<String, ?> redisOperations(ReactiveRedisConnectionFactory factory);
      }

      interface Strategy {
        Record<String, ?> create(long userId, long counterId, long value);

        Optional<ChangeValue> read(Record<String, ?> record);

        ReactiveRedisTemplate<String, ?> redisOperations(ReactiveRedisConnectionFactory factory);
      }
    }
  }
}
