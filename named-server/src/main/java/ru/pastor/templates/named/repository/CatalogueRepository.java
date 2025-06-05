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

/**
 * Репозиторий для работы с каталогом счетчиков в базе данных.
 * Предоставляет методы для получения элементов каталога.
 */
public interface CatalogueRepository {

  /**
   * Получает список всех элементов каталога с возможностью фильтрации.
   *
   * @param filter фильтр для выборки элементов каталога
   * @return поток элементов каталога, соответствующих фильтру
   */
  Flux<CatalogueEntity> counters(Filter filter);

  /**
   * Получает элемент каталога по его имени.
   *
   * @param name имя элемента каталога
   * @return элемент каталога в виде Mono или пустой Mono, если элемент не найден
   */
  Mono<CatalogueEntity> get(String name);

  Mono<CatalogueEntity> create(String name, String description);

  /**
   * Запись для фильтрации элементов каталога.
   * В текущей реализации не содержит параметров фильтрации.
   */
  record Filter() {
  }

  /**
   * Реализация репозитория каталога для PostgreSQL с использованием реактивного доступа к данным.
   */
  @Slf4j
  @RequiredArgsConstructor
  @Service("CatalogueRepository.Postgres")
  class Postgres implements CatalogueRepository {
    /**
     * Клиент для работы с базой данных в реактивном стиле.
     */
    private final DatabaseClient client;

    /**
     * Оператор транзакций для обеспечения атомарности операций.
     */
    private final TransactionalOperator tx;

    /**
     * Преобразует строку результата запроса в объект CatalogueEntity.
     *
     * @param row      строка результата запроса
     * @param metadata метаданные строки
     * @return объект CatalogueEntity с данными из строки результата
     */
    private static CatalogueEntity map(Row row, RowMetadata metadata) {
      return new CatalogueEntity(
        row.get("id", Integer.class),
        row.get("name", String.class),
        row.get("description", String.class),
        row.get("created", LocalDateTime.class),
        row.get("updated", LocalDateTime.class)
      );
    }

    /**
     * {@inheritDoc}
     * Получает все элементы каталога из базы данных.
     * Фильтрация в текущей реализации не используется.
     */
    @Override
    public Flux<CatalogueEntity> counters(Filter filter) {
      return client
        .sql("SELECT id, name, description, created, updated FROM counter_catalogue")
        .map(Postgres::map)
        .all()
        .as(tx::transactional);
    }

    /**
     * {@inheritDoc}
     * Получает элемент каталога по имени из базы данных.
     * Возвращает первый найденный элемент или пустой Mono, если элемент не найден.
     */
    @Override
    public Mono<CatalogueEntity> get(String name) {
      return client
        .sql("SELECT id, name, description, created, updated FROM counter_catalogue WHERE name = :name")
        .bind("name", name)
        .map(Postgres::map)
        .first()
        .as(tx::transactional);
    }

    @Override
    public Mono<CatalogueEntity> create(String name, String description) {
      return client.sql("INSERT INTO counter_catalogue(name, description) VALUES(:name, :description)")
        .bind("name", name)
        .bind("description", description)
        .filter(stmt -> stmt.returnGeneratedValues("id"))
        .map(row -> CatalogueEntity.builder()
          .id(row.get("id", Integer.class))
          .name(name)
          .description(description)
          .build())
        .one()
        .as(tx::transactional);
    }
  }

}
