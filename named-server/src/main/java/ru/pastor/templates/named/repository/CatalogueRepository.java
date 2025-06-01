package ru.pastor.templates.named.repository;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.repository.entity.CatalogueEntity;

import java.time.LocalDateTime;

public interface CatalogueRepository {

  Flux<CatalogueEntity> counters(Filter filter);

  Mono<CatalogueEntity> get(String name);

  record Filter() {

  }

  @Slf4j
  @RequiredArgsConstructor
  @Service("CatalogueRepository.Postgres")
  class Postgres implements CatalogueRepository {
    private final DatabaseClient client;
    private final TransactionalOperator tx;

    private static CatalogueEntity map(Row row, RowMetadata metadata) {
      return new CatalogueEntity(
        row.get("id", Long.class),
        row.get("name", String.class),
        row.get("description", String.class),
        row.get("created", LocalDateTime.class),
        row.get("updated", LocalDateTime.class)
      );
    }

    @Override
    public Flux<CatalogueEntity> counters(Filter filter) {
      return client
        .sql("SELECT id, name, description, created, updated FROM counter_catalogue")
        .map(Postgres::map)
        .all()
        .as(tx::transactional);
    }

    @Override
    public Mono<CatalogueEntity> get(String name) {
      return client
        .sql("SELECT id, name, description, created, updated FROM counter_catalogue WHERE name = :name")
        .bind("name", name)
        .map(Postgres::map)
        .one()
        .as(tx::transactional);
    }
  }

}
