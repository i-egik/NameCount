package ru.pastor.templates.named.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Интерфейс для работы с именованным кэшем.
 * Предоставляет методы для получения, удаления и обновления значений в кэше.
 *
 * @param <K> тип ключа
 * @param <V> тип значения
 */
public interface NamedCache<K, V> {

  /**
   * Получает значение из кэша по ключу.
   *
   * @param key ключ для поиска значения
   * @return значение в виде Mono или пустой Mono, если значение не найдено
   */
  Mono<V> get(K key);

  /**
   * Удаляет значение из кэша по ключу.
   *
   * @param key ключ для удаления значения
   * @return пустой Mono, сигнализирующий о завершении операции
   */
  Mono<Void> delete(K key);

  /**
   * Обновляет значение в кэше по ключу.
   *
   * @param key   ключ для обновления значения
   * @param value новое значение
   * @return пустой Mono, сигнализирующий о завершении операции
   */
  Mono<Void> update(K key, V value);

  /**
   * Интерфейс для загрузки всех значений кэша.
   *
   * @param <K> тип ключа
   * @param <V> тип значения
   */
  interface All<K, V> {
    /**
     * Применяет функцию ко всем парам ключ-значение.
     *
     * @param entry функция, применяемая к каждой паре ключ-значение
     * @return пустой Mono, сигнализирующий о завершении операции
     */
    Mono<Void> all(BiFunction<K, V, Void> entry);
  }

  /**
   * Абстрактный класс для реализации кэша только для чтения.
   * Методы delete и update возвращают пустой Mono.
   *
   * @param <K> тип ключа
   * @param <V> тип значения
   */
  @SuppressWarnings("AbstractClassName")
  abstract class ReadOnly<K, V> implements NamedCache<K, V> {

    /**
     * {@inheritDoc}
     * В реализации только для чтения метод всегда возвращает пустой Mono.
     */
    @Override
    public final Mono<Void> delete(K key) {
      return Mono.empty();
    }

    /**
     * {@inheritDoc}
     * В реализации только для чтения метод всегда возвращает пустой Mono.
     */
    @Override
    public final Mono<Void> update(K key, V value) {
      return Mono.empty();
    }
  }

  /**
   * Локальная реализация кэша с использованием Guava Cache.
   * Поддерживает делегирование запросов другому кэшу и метрики.
   *
   * @param <K> тип ключа
   * @param <V> тип значения
   */
  final class Local<K, V> implements NamedCache<K, V> {
    /**
     * Внутренний кэш на основе Guava Cache.
     */
    private final Cache<K, V> cache;

    /**
     * Делегат для получения значений, отсутствующих в локальном кэше.
     */
    private final NamedCache<K, V> delegate;

    /**
     * Создает новый локальный кэш с указанными параметрами.
     *
     * @param name      имя кэша для метрик
     * @param expires   время жизни элементов кэша
     * @param registry  реестр метрик
     * @param delegate  делегат для получения отсутствующих значений
     * @param all       источник для предварительной загрузки всех значений
     */
    public Local(String name,
                 Duration expires,
                 MeterRegistry registry,
                 NamedCache<K, V> delegate,
                 All<K, V> all) {
      this.cache = CacheBuilder.newBuilder()
        .recordStats()
        .weakValues()
        .expireAfterWrite(expires.toSeconds(), TimeUnit.SECONDS)
        .build();
      this.delegate = delegate;
      Gauge.builder("Cache_" + name + "_hit_count", () -> cache.stats().hitCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_miss_count", () -> cache.stats().missCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_eviction_count", () -> cache.stats().evictionCount())
        .register(registry);
      Gauge.builder("Cache_" + name + "_total_load_time", () -> cache.stats().totalLoadTime())
        .register(registry);
      if (all != null) {
        all.all((k, v) -> {
          cache.put(k, v);
          return null;
        }).subscribe();
      }
    }

    /**
     * {@inheritDoc}
     * Получает значение из локального кэша, а при отсутствии - из делегата.
     * Полученное от делегата значение сохраняется в локальном кэше.
     */
    @Override
    public Mono<V> get(K key) {
      return CacheMono.lookup(k -> Mono.justOrEmpty(cache.getIfPresent(key)).map(Signal::next), key)
        .onCacheMissResume(() -> delegate.get(key).cache())
        .andWriteWith((k, signal) -> Mono.fromRunnable(() -> Optional.ofNullable(signal.get())
          .ifPresent(value -> cache.put(key, value))));
    }

    /**
     * {@inheritDoc}
     * Удаляет значение из делегата, а затем из локального кэша.
     */
    @Override
    public Mono<Void> delete(K key) {
      return delegate.delete(key).then(Mono.fromRunnable(() -> cache.invalidate(key)));
    }

    /**
     * {@inheritDoc}
     * Обновляет значение в делегате, а затем в локальном кэше.
     */
    @Override
    public Mono<Void> update(K key, V value) {
      return delegate.update(key, value)
        .then(Mono.fromRunnable(() -> cache.put(key, value)));
    }
  }
}
