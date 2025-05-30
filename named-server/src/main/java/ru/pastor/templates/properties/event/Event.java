package ru.pastor.templates.properties.event;

public interface Event<T> {

  static <T> Event<T> of(String namespace, T source, Type type) {
    return new Event<>() {
      @Override
      public String namespace() {
        return namespace;
      }

      @Override
      public T source() {
        return source;
      }

      @Override
      public Type type() {
        return type;
      }
    };
  }

  String namespace();

  T source();

  Type type();

  enum Type {
    UPDATE, DELETE
  }

  interface Listener {
    <T> void onEvent(Event<T> event);
  }
}
