package ru.pastor.templates.named.repository;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.repository.entity.CatalogueEntity;
import ru.pastor.templates.named.repository.entity.CounterEntity;

import java.time.LocalDateTime;

public interface CounterRepository {

  Mono<CounterEntity> get(long counterId, long userId);

  Mono<CounterEntity> create(long counterId, long userId, long initialValue);

  Mono<CounterEntity> update(long counterId, long userId, long newValue);

  default Mono<CounterEntity> updateOrCreate(long counterId, long userId, long newValue) {
    return update(counterId, userId, newValue)
      .switchIfEmpty(create(counterId, userId, newValue));
  }

  @Slf4j
  @RequiredArgsConstructor
  @Service("CounterRepository.Postgres")
  class Postgres implements CounterRepository {
    private final DatabaseClient client;
    private final TransactionalOperator tx;

    private static CounterEntity map(Row row, RowMetadata metadata) {
      CatalogueEntity catalogue = new CatalogueEntity(
        row.get("counter_id", Long.class).intValue(),
        row.get("name", String.class),
        row.get("description", String.class),
        row.get("catalogue_created", LocalDateTime.class),
        row.get("catalogue_updated", LocalDateTime.class)
      );

      return CounterEntity.builder()
        .id(row.get("id", Integer.class))
        .catalogue(catalogue)
        .userId(row.get("user_id", Long.class))
        .value(row.get("value", Long.class))
        .created(row.get("created", LocalDateTime.class))
        .updated(row.get("updated", LocalDateTime.class))
        .build();
    }

    @Override
    public Mono<CounterEntity> get(long counterId, long userId) {
      return client
        .sql("SELECT cv.id, cv.counter_id, cv.user_id, cv.\"value\", cv.created, cv.updated, " +
          "cc.name, cc.description, cc.created as catalogue_created, cc.updated as catalogue_updated " +
          "FROM counter_values cv " +
          "JOIN counter_catalogue cc ON cv.counter_id = cc.id " +
          "WHERE cv.counter_id = :counterId AND cv.user_id = :userId")
        .bind("counterId", counterId)
        .bind("userId", userId)
        .map(Postgres::map)
        .first()
        .as(tx::transactional);
    }

    @Override
    public Mono<CounterEntity> create(long counterId, long userId, long initialValue) {
      LocalDateTime now = LocalDateTime.now();
      return client
        .sql("INSERT INTO counter_values (counter_id, user_id, \"value\", created, updated) " +
          "VALUES (:counterId, :userId, :value, :created, :updated) ")
        .bind("counterId", counterId)
        .bind("userId", userId)
        .bind("value", initialValue)
        .bind("created", now)
        .bind("updated", now)
        .filter(stmt -> stmt.returnGeneratedValues("id"))
        .map(row -> row.get("id", Integer.class))
        .one()
        .flatMap(id -> get(counterId, userId))
        .as(tx::transactional);
    }

    @Override
    public Mono<CounterEntity> update(long counterId, long userId, long newValue) {
      return client
        .sql("UPDATE counter_values SET \"value\" = :value, updated = :updated " +
          "WHERE counter_id = :counterId AND user_id = :userId ")
        .bind("counterId", counterId)
        .bind("userId", userId)
        .bind("value", newValue)
        .bind("updated", LocalDateTime.now())
        .fetch().rowsUpdated()
        .flatMap(id -> get(counterId, userId))
        .switchIfEmpty(create(counterId, userId, newValue))
        .as(tx::transactional);
    }
  }
}
