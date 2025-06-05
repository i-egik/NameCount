package ru.pastor.templates.named.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.server.grpc.CountFilter;
import ru.pastor.templates.named.server.grpc.CountPutRequest;
import ru.pastor.templates.named.server.grpc.CountValue;
import ru.pastor.templates.named.server.grpc.Error;
import ru.pastor.templates.named.server.grpc.ReactorCountServiceGrpc;
import ru.pastor.templates.named.server.grpc.Status;

/**
 * gRPC сервис для работы со счетчиками.
 * Предоставляет API для получения и обновления значений счетчиков через gRPC протокол.
 * Реализует базовый класс ReactorCountServiceGrpc.CountServiceImplBase для реактивной обработки gRPC запросов.
 * Делегирует бизнес-логику сервису NamedCountService.
 */
@Slf4j
@RequiredArgsConstructor
@Service("NamedServerService.Count.Grpc")
public class NamedServerCountServiceGrpc extends ReactorCountServiceGrpc.CountServiceImplBase {
  /**
   * Сервис для работы с именованными счетчиками.
   * Используется для выполнения операций с счетчиками.
   */
  private final NamedCountService namedCountService;

  /**
   * Получает значение счетчика по имени и идентификатору пользователя.
   *
   * @param request запрос с параметрами фильтрации (имя счетчика и идентификатор пользователя)
   * @return ответ, содержащий значение счетчика и статус операции
   */
  @Override
  public Mono<CountValue> get(CountFilter request) {
    return namedCountService.get(request.getName(), request.getUserId())
      .map(v -> CountValue.newBuilder()
        .setName(request.getName())
        .setValue(v)
        .setStatus(Status.SUCCESS)
        .build())
      .switchIfEmpty(Mono.just(CountValue.newBuilder()
        .setStatus(Status.NOT_FOUND)
        .build()))
      .onErrorResume(throwable -> Mono.just(CountValue.newBuilder()
        .setStatus(Status.FAILURE)
        .setError(Error.newBuilder().setMessage(throwable.getMessage()).build())
        .build()));
  }

  /**
   * Получает список счетчиков по параметрам фильтрации.
   * В текущей реализации делегирует вызов родительскому методу, который возвращает пустой поток.
   *
   * @param request запрос с параметрами фильтрации
   * @return поток ответов, содержащих значения счетчиков и статусы операций
   */
  @Override
  public Flux<CountValue> list(CountFilter request) {
    return super.list(request);
  }

  /**
   * Обновляет значение счетчика, увеличивая его на указанную величину.
   *
   * @param request запрос с параметрами обновления (имя счетчика, идентификатор пользователя и величина изменения)
   * @return ответ, содержащий новое значение счетчика и статус операции
   */
  @Override
  public Mono<CountValue> put(CountPutRequest request) {
    return namedCountService.increment(request.getName(), request.getUserId(), request.getDelta())
      .map(v -> CountValue.newBuilder()
        .setName(request.getName())
        .setValue(v)
        .setStatus(Status.SUCCESS)
        .build())
      .switchIfEmpty(Mono.just(CountValue.newBuilder()
        .setStatus(Status.NOT_FOUND)
        .build()))
      .onErrorResume(throwable -> Mono.just(CountValue.newBuilder()
        .setStatus(Status.FAILURE)
        .setError(Error.newBuilder().setMessage(throwable.getMessage()).build())
        .build()));
  }
}
