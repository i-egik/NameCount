package ru.pastor.templates.named.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.cache.NamedCache;
import ru.pastor.templates.named.mapper.CatalogueMapper;
import ru.pastor.templates.named.model.CatalogueModel;
import ru.pastor.templates.named.repository.CatalogueRepository;

import java.util.Optional;

/**
 * Сервис для работы с каталогом счетчиков.
 * Предоставляет методы для получения списка счетчиков и создания/обновления счетчиков.
 */
public interface NamedCatalogueService {

  /**
   * Получает информацию о счетчике по его имени.
   *
   * @param name имя счетчика
   * @return информация о счетчике в виде Mono
   */
  Mono<CatalogueModel> get(String name);

  /**
   * Получает список всех счетчиков.
   *
   * @return поток информации о счетчиках в виде Flux
   */
  Flux<CatalogueModel> list();

  /**
   * Создает или обновляет счетчик с указанным именем и описанием.
   *
   * @param name        имя счетчика
   * @param description описание счетчика
   * @return информация о созданном/обновленном счетчике в виде Mono
   */
  Mono<CatalogueModel> createOrUpdate(String name, String description, Long defaultValue);

  Mono<CatalogueModel> update(long id,
                              Optional<String> name,
                              Optional<String> description,
                              Optional<Long> defaultValue
                              );

  /**
   * Стандартная реализация сервиса каталога счетчиков.
   * Использует кэш для хранения информации о счетчиках и репозиторий для доступа к базе данных.
   */
  @Slf4j
  @RequiredArgsConstructor
  final class Standard implements NamedCatalogueService {
    /**
     * Кэш для хранения каталога счетчиков.
     */
    private final NamedCache<String, Integer> catalogue;

    /**
     * Репозиторий для доступа к каталогу счетчиков в базе данных.
     */
    private final CatalogueRepository catalogueRepository;

    /**
     * {@inheritDoc}
     * Получает информацию о счетчике из кэша по его имени.
     * Если информация отсутствует в кэше, обращается к репозиторию.
     */
    @Override
    public Mono<CatalogueModel> get(String name) {
      return catalogue.get(name)
        .map(id -> CatalogueModel.builder()
          .information(CatalogueModel.Information.builder()
            .id(id.longValue())
            .name(name)
            .build())
          .build())
        .switchIfEmpty(catalogueRepository.get(name)
          .flatMap(entity -> catalogue.update(name, entity.id())
            .thenReturn(CatalogueMapper.INSTANCE.toModel(entity))));
    }

    /**
     * {@inheritDoc}
     * Получает список всех счетчиков из репозитория.
     */
    @Override
    public Flux<CatalogueModel> list() {
      return catalogueRepository.counters(new CatalogueRepository.Filter())
        .map(CatalogueMapper.INSTANCE::toModel);
    }

    /**
     * {@inheritDoc}
     * Создает или обновляет счетчик с указанным именем и описанием.
     */
    @Override
    public Mono<CatalogueModel> createOrUpdate(String name, String description, Long defaultValue) {
      return catalogueRepository.get(name)
        .flatMap(entity -> {
          //FIXME: Если такой счетчик в каталоге существует, то надо его обновить
          //       сделать merge по полям
          int id = entity.id();
          return catalogue.update(name, id)
            .thenReturn(CatalogueMapper.INSTANCE.toModel(entity));
        })
        .switchIfEmpty(Mono.defer(() -> catalogueRepository.create(name, description, defaultValue)
          .flatMap(entity -> catalogue.update(name, entity.id())
            .thenReturn(CatalogueMapper.INSTANCE.toModel(entity)))));
    }

    @Override
    public Mono<CatalogueModel> update(long id, Optional<String> name, Optional<String> description,
                                       Optional<Long> defaultValue) {
      return catalogueRepository.update(id, name.orElse(null),
          description.orElse(null), defaultValue.orElse(null))
        .map(CatalogueMapper.INSTANCE::toModel)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Can't update")));
    }
  }
}
