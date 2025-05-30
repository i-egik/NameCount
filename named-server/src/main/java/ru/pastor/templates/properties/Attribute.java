package ru.pastor.templates.properties;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

public interface Attribute {

  <T> Optional<T> value(Map<String, Object> attributes);

  @Slf4j
  enum Default implements Attribute {
    EXPIRES("expires"),
    /**
     * Обновление параметра в секундах.
     * По истечению времени в параметре будет осуществлена попытка получить новое значение из репозитория
     */
    REFRESH("refresh");
    private final String name;

    Default(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> value(Map<String, Object> attributes) {
      Object v = attributes.get(name);
      try {
        return Optional.ofNullable((T) v);
      } catch (ClassCastException e) {
        if (log.isTraceEnabled()) {
          log.trace("Attribute {} is not of type {}", name, v);
        }
        return Optional.empty();
      }
    }
  }
}
