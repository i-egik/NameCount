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

@Slf4j
@RequiredArgsConstructor
@Service("NamedServerService.Count.Grpc")
public class NamedServerCountServiceGrpc extends ReactorCountServiceGrpc.CountServiceImplBase {
  private final NamedCountService namedCountService;

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

  @Override
  public Flux<CountValue> list(CountFilter request) {
    return super.list(request);
  }

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
