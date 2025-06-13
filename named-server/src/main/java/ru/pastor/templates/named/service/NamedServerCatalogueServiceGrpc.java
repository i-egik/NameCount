package ru.pastor.templates.named.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.pastor.templates.named.server.grpc.CatalogueFilter;
import ru.pastor.templates.named.server.grpc.CatalogueInformation;
import ru.pastor.templates.named.server.grpc.CataloguePutRequest;
import ru.pastor.templates.named.server.grpc.CatalogueReplyList;
import ru.pastor.templates.named.server.grpc.CatalogueReplyValue;
import ru.pastor.templates.named.server.grpc.CatalogueUpdateRequest;
import ru.pastor.templates.named.server.grpc.Error;
import ru.pastor.templates.named.server.grpc.ReactorCatalogueServiceGrpc;
import ru.pastor.templates.named.server.grpc.Status;

import java.util.Optional;

/**
 * gRPC сервис для работы с каталогом счетчиков.
 * Предоставляет API для доступа к каталогу счетчиков через gRPC протокол.
 * Расширяет базовый класс ReactorCatalogueServiceGrpc.CatalogueServiceImplBase для реактивной обработки gRPC запросов.
 * Делегирует бизнес-логику сервису NamedCatalogueService.
 */
@Slf4j
@RequiredArgsConstructor
@Service("NamedServerService.Catalogue.Grpc")
public class NamedServerCatalogueServiceGrpc extends ReactorCatalogueServiceGrpc.CatalogueServiceImplBase {

  /**
   * Сервис для работы с каталогом счетчиков.
   * Используется для выполнения операций с каталогом.
   */
  private final NamedCatalogueService namedCatalogueService;

  @Override
  public Mono<CatalogueReplyValue> update(CatalogueUpdateRequest request) {
    long id = request.getId();
    return namedCatalogueService.update(id,
      request.hasNewName() ? Optional.of(request.getNewName()) : Optional.empty(),
      request.hasNewName() ? Optional.of(request.getNewDescription()) : Optional.empty())
      .map(model -> CatalogueReplyValue.newBuilder()
        .setValue(CatalogueInformation.newBuilder()
          .setId(model.information().id())
          .setName(model.information().name())
          .build())
        .setStatus(Status.SUCCESS)
        .build())
      .switchIfEmpty(Mono.just(CatalogueReplyValue.newBuilder()
        .setStatus(Status.NOT_FOUND)
        .build()))
      .onErrorResume(throwable -> Mono.just(CatalogueReplyValue.newBuilder()
        .setStatus(Status.FAILURE)
        .setError(Error.newBuilder().setMessage(throwable.getMessage()).build())
        .build()));
  }

  /**
   * Получает список счетчиков по параметрам фильтрации.
   *
   * @param request запрос с параметрами фильтрации
   * @return поток ответов, содержащих информацию о счетчиках и статусы операций
   */
  @Override
  public Mono<CatalogueReplyList> list(CatalogueFilter request) {
    if (request.hasName()) {
      return namedCatalogueService.get(request.getName())
        .map(model -> CatalogueReplyList.newBuilder()
          .setStatus(Status.SUCCESS)
          .addValues(CatalogueInformation.newBuilder()
            .setId(model.information().id())
            .setName(model.information().name())
            .build())
          .build())
        .switchIfEmpty(Mono.just(CatalogueReplyList.newBuilder()
          .setStatus(Status.NOT_FOUND)
          .build()))
        .onErrorResume(throwable -> Mono.just(CatalogueReplyList.newBuilder()
          .setStatus(Status.FAILURE)
          .setError(Error.newBuilder().setMessage(throwable.getMessage()).build())
          .build()));
    } else {
      return namedCatalogueService.list()
        .map(model -> CatalogueInformation.newBuilder()
          .setId(model.information().id())
          .setName(model.information().name())
          .build())
        .collectList().map(values -> CatalogueReplyList.newBuilder()
          .setStatus(Status.SUCCESS)
          .addAllValues(values)
          .build())
        .switchIfEmpty(Mono.just(CatalogueReplyList.newBuilder()
          .setStatus(Status.NOT_FOUND)
          .build()))
        .onErrorResume(throwable -> Mono.just(CatalogueReplyList.newBuilder()
          .setStatus(Status.FAILURE)
          .setError(Error.newBuilder().setMessage(throwable.getMessage()).build())
          .build()));
    }
  }

  /**
   * Создает или обновляет счетчик в каталоге.
   *
   * @param request запрос с параметрами создания/обновления счетчика
   * @return ответ, содержащий информацию о созданном/обновленном счетчике и статус операции
   */
  @Override
  public Mono<CatalogueReplyValue> put(CataloguePutRequest request) {
    if (request.hasName()) {
      return namedCatalogueService.createOrUpdate(request.getName(), request.getDescription())
        .map(model -> CatalogueReplyValue.newBuilder()
          .setValue(CatalogueInformation.newBuilder()
            .setId(model.information().id())
            .setName(model.information().name())
            .build())
          .setStatus(Status.SUCCESS)
          .build())
        .switchIfEmpty(Mono.just(CatalogueReplyValue.newBuilder()
          .setStatus(Status.NOT_FOUND)
          .build()))
        .onErrorResume(throwable -> Mono.just(CatalogueReplyValue.newBuilder()
          .setStatus(Status.FAILURE)
          .setError(Error.newBuilder().setMessage(throwable.getMessage()).build())
          .build()));
    } else if (request.hasId()) {
      return Mono.just(CatalogueReplyValue.newBuilder()
        .setStatus(Status.FAILURE)
        .setError(Error.newBuilder().setMessage("Id is not provided").build())
        .build());
    } else {
      return Mono.just(CatalogueReplyValue.newBuilder()
        .setStatus(Status.FAILURE)
        .setError(Error.newBuilder().setMessage("Neither name nor id provided").build())
        .build());
    }
  }
}
