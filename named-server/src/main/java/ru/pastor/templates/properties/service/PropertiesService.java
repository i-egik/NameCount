package ru.pastor.templates.properties.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.pastor.templates.properties.Property;
import ru.pastor.templates.properties.event.Event;
import ru.pastor.templates.properties.repository.PropertyRepository;

import java.util.List;
import java.util.Map;

public interface PropertiesService {

  Flux<Property> getProperties(String namespace);

  Mono<Void> createOrUpdate(String namespace, Property property);

  Mono<Property> get(String namespace, String key);

  Mono<Void> delete(String namespace, String key);

  @Service("PropertiesService.Default")
  class Default implements PropertiesService {
    private final Map<String, PropertyRepository> repositories;
    private final List<Event.Listener> listeners;

    @Autowired
    public Default(Map<String, PropertyRepository> repositories, List<Event.Listener> listeners) {
      this.repositories = repositories;
      this.listeners = listeners;
    }

    @Override
    public Flux<Property> getProperties(String namespace) {
      PropertyRepository repository = repositories.get(namespace);
      if (repository == null) {
        return Flux.empty();
      }
      return repository.listProperties(PropertyRepository.Page.of());
    }

    @Override
    public Mono<Void> createOrUpdate(String namespace, Property property) {
      PropertyRepository repository = repositories.get(namespace);
      if (repository == null) {
        return Mono.empty();
      }
      return repository.createOrUpdate(property)
        .doOnSuccess(v -> fireEvent(Event.of(namespace, property, Event.Type.UPDATE)))
        .then();
    }

    @Override
    public Mono<Property> get(String namespace, String key) {
      PropertyRepository repository = repositories.get(namespace);
      if (repository == null) {
        return Mono.empty();
      }
      return repository.fetch(key);
    }

    @Override
    public Mono<Void> delete(String namespace, String key) {
      PropertyRepository repository = repositories.get(namespace);
      if (repository == null) {
        return Mono.empty();
      }
      return repository.delete(key)
        .doOnSuccess(v -> fireEvent(Event.of(namespace, key, Event.Type.DELETE)));
    }

    private <T> void fireEvent(Event<T> event) {
      for (Event.Listener listener : listeners) {
        listener.onEvent(event);
      }
    }
  }
}
