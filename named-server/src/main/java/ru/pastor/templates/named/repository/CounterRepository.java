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

/**
 * Репозиторий для работы со счетчиками в базе данных.
 * Предоставляет методы для получения, создания и обновления счетчиков.
 */
public interface CounterRepository {

  /**
   * Получает счетчик по идентификатору счетчика и пользователя.
   *
   * @param counterId идентификатор счетчика
   * @param userId    идентификатор пользователя
   * @return объект счетчика в виде Mono или пустой Mono, если счетчик не найден
   */
  Mono<CounterEntity> get(long counterId, long userId);

  /**
   * Создает новый счетчик с указанным начальным значением.
   *
   * @param counterId    идентификатор счетчика
   * @param userId       идентификатор пользователя
   * @param initialValue начальное значение счетчика
   * @return созданный объект счетчика в виде Mono
   */
  Mono<CounterEntity> create(long counterId, long userId, long initialValue);

  /**
   * Обновляет значение существующего счетчика.
   *
   * @param counterId идентификатор счетчика
   * @param userId    идентификатор пользователя
   * @param newValue  новое значение счетчика
   * @return обновленный объект счетчика в виде Mono или пустой Mono, если счетчик не найден
   */
  Mono<CounterEntity> update(long counterId, long userId, long newValue);

  /**
   * Обновляет существующий счетчик или создает новый, если счетчик не найден.
   *
   * @param counterId идентификатор счетчика
   * @param userId    идентификатор пользователя
   * @param newValue  новое значение счетчика
   * @return обновленный или созданный объект счетчика в виде Mono
   */
  default Mono<CounterEntity> updateOrCreate(long counterId, long userId, long newValue) {
    return update(counterId, userId, newValue)
      .switchIfEmpty(create(counterId, userId, newValue));
  }

  /**
   * Реализация репозитория счетчиков для PostgreSQL с использованием реактивного доступа к данным.
   */
  @Slf4j
  @RequiredArgsConstructor
  @Service("CounterRepository.Postgres")
  class Postgres implements CounterRepository {
    /**
     * Клиент для работы с базой данных в реактивном стиле.
     */
    private final DatabaseClient client;

    /**
     * Оператор транзакций для обеспечения атомарности операций.
     */
    private final TransactionalOperator tx;

    /**
     * Преобразует строку результата запроса в объект CounterEntity.
     *
     * @param row      строка результата запроса
     * @param metadata метаданные строки
     * @return объект CounterEntity с данными из строки результата
     */
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

    /**
     * {@inheritDoc}
     * Получает счетчик из базы данных, объединяя данные из таблиц counter_values и counter_catalogue.
     */
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

    /**
     * {@inheritDoc}
     * Создает новую запись счетчика в базе данных и возвращает созданный объект.
     */
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

    /**
     * {@inheritDoc}
     * Обновляет значение счетчика в базе данных и возвращает обновленный объект.
     * Если счетчик не найден, создает новый с указанным значением.
     */
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
