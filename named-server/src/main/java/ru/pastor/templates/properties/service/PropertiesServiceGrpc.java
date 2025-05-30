package ru.pastor.templates.properties.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.pastor.templates.properties.Property;
import ru.pastor.templates.properties.server.grpc.DeleteRequest;
import ru.pastor.templates.properties.server.grpc.ReactorServiceGrpc;
import ru.pastor.templates.properties.server.grpc.Result;
import ru.pastor.templates.properties.server.grpc.ScopedProperty;
import ru.pastor.templates.properties.server.grpc.ScopedReply;
import ru.pastor.templates.properties.server.grpc.ScopedRequest;
import ru.pastor.templates.properties.server.grpc.Status;
import ru.pastor.templates.properties.server.grpc.UpdateRequest;

import java.util.HashMap;
import java.util.Map;

@Service("PropertiesService.Grpc")
public class PropertiesServiceGrpc extends ReactorServiceGrpc.ServiceImplBase {
  private final PropertiesService service;
  private final ObjectMapper mapper;

  @Autowired
  public PropertiesServiceGrpc(PropertiesService service, ObjectMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  public Mono<ScopedReply> getScopedValue(ScopedRequest request) {
    String namespace = request.getNamespace();
    if (request.hasKey()) {
      return service.get(namespace, request.getKey()).map(property -> {
        ScopedReply.Builder builder = ScopedReply.newBuilder();
        builder.setResult(Result.newBuilder().setStatus(Status.SUCCESS).build());
        builder.addProperty(map(property));
        return builder.build();
      });
    }
    return service.getProperties(namespace).map(PropertiesServiceGrpc::map).collectList().map(list -> {
      ScopedReply.Builder builder = ScopedReply.newBuilder();
      builder.setResult(Result.newBuilder().setStatus(Status.SUCCESS).build());
      list.forEach(builder::addProperty);
      return builder.build();
    });
  }

  @Override
  public Mono<Result> updScopedValue(UpdateRequest request) {
    return service.createOrUpdate(request.getNamespace(), map(request.getProperty()))
      .thenReturn(Result.newBuilder().setStatus(Status.SUCCESS).build());
  }

  @Override
  public Mono<Result> delScopedValue(DeleteRequest request) {
    return service.delete(request.getNamespace(), request.getKey())
      .thenReturn(Result.newBuilder().setStatus(Status.SUCCESS).build());
  }

  private static ScopedProperty map(Property property) {
    return ScopedProperty.newBuilder().setKey(property.key()).setValue(property.asString()).build();
  }

  private Property map(ScopedProperty property) {
    Map<String, Object> attributes = Map.of();
    if (property.getAttributesCount() != 0) {
      attributes = new HashMap<>(property.getAttributesCount());
      attributes.putAll(property.getAttributesMap());
    }
    return Property.of(property.getKey(), property.getValue(), mapper, attributes);
  }
}
