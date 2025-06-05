package ru.pastor.templates.named.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.cache.NamedCache;

/**
 * Сервис для работы с именованными счетчиками.
 * Предоставляет методы для получения и увеличения значений счетчиков для конкретных пользователей.
 */
public interface NamedCountService {

  /**
   * Получает текущее значение счетчика для указанного пользователя.
   *
   * @param name   имя счетчика
   * @param userId идентификатор пользователя
   * @return текущее значение счетчика в виде Mono
   */
  Mono<Long> get(String name, long userId);

  /**
   * Увеличивает значение счетчика на 1 для указанного пользователя.
   *
   * @param name   имя счетчика
   * @param userId идентификатор пользователя
   * @return новое значение счетчика после увеличения в виде Mono
   */
  Mono<Long> increment(String name, long userId);

  /**
   * Увеличивает значение счетчика на указанную величину для указанного пользователя.
   *
   * @param name   имя счетчика
   * @param userId идентификатор пользователя
   * @param delta  величина, на которую нужно увеличить счетчик
   * @return новое значение счетчика после увеличения в виде Mono
   */
  Mono<Long> increment(String name, long userId, long delta);

  /**
   * Стандартная реализация сервиса именованных счетчиков.
   * Использует кэши для хранения значений счетчиков и каталога счетчиков.
   */
  @Slf4j
  @RequiredArgsConstructor
  final class Standard implements NamedCountService {
    /**
     * Кэш для хранения значений счетчиков.
     */
    private final NamedCache<String, Long> values;

    /**
     * Кэш для хранения каталога счетчиков.
     */
    private final NamedCache<String, Long> catalogue;

    /**
     * Формирует ключ для доступа к значению счетчика.
     *
     * @param name   имя счетчика
     * @param userId идентификатор пользователя
     * @return ключ в формате "named:userId:counterId" в виде Mono
     */
    private Mono<String> key(String name, long userId) {
      return catalogue.get(name).map(v -> String.format("named:%d:%d", userId, v));
    }

    /**
     * {@inheritDoc}
     * Получает значение счетчика из кэша по сформированному ключу.
     */
    @Override
    public Mono<Long> get(String name, long userId) {
      return key(name, userId).flatMap(values::get);
    }

    /**
     * {@inheritDoc}
     * Делегирует вызов методу increment с дельтой равной 1.
     */
    @Override
    public Mono<Long> increment(String name, long userId) {
      return increment(name, userId, 1);
    }

    /**
     * {@inheritDoc}
     * Увеличивает значение счетчика на указанную величину.
     * Если счетчик не существует, создает его с начальным значением 0 и затем увеличивает.
     */
    @Override
    public Mono<Long> increment(String name, long userId, long delta) {
      return key(name, userId).flatMap(k -> values.get(k)
        .defaultIfEmpty(0L)
        .flatMap(value -> values.update(k, value + delta).thenReturn(value + delta)));
    }
  }
}
