package ru.pastor.templates.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("AbstractClassName")
@ToString(of = {"key", "value"})
@EqualsAndHashCode(of = "key")
@Slf4j
public abstract class Property {
  private final String key;
  private final String value;
  private final Map<String, Object> attributes;
  private final ObjectMapper mapper;

  protected Property(String key, String value, Map<String, Object> attributes, ObjectMapper mapper) {
    this.key = key;
    this.value = value;
    this.attributes = attributes;
    this.mapper = mapper;
  }

  public final String key() {
    return key;
  }

  public static Property of(String key, String value) {
    return of(key, value, null);
  }

  public static Property of(String key, String value, ObjectMapper mapper) {
    return of(key, value, mapper, Map.of());
  }

  public static Property of(String key, String value, ObjectMapper mapper, Map<String, Object> attributes) {
    return new Default(key, value, attributes, mapper);
  }

  public <T> Optional<T> attribute(Attribute attribute) {
    return attribute.value(attributes);
  }

  public <T> Optional<T> asObject(Class<T> klass) {
    if (mapper == null) {
      if (log.isDebugEnabled()) {
        log.debug("Mapper is null. Returning empty optional");
      }
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(mapper.readValue(value, klass));
    } catch (JsonProcessingException e) {
      if (log.isDebugEnabled()) {
        log.debug("Error parsing JSON object", e);
      }
      return Optional.empty();
    }
  }

  public boolean asBoolean() {
    return asBoolean(false);
  }

  public boolean asBoolean(boolean defaultValue) {
    return value == null ? defaultValue : Boolean.parseBoolean(value);
  }

  public String asString() {
    return asString(null);
  }

  public String asString(String defaultValue) {
    return value == null ? defaultValue : value;
  }

  public int asInt(int defaultValue) {
    return (int) asLong(defaultValue);
  }

  public long asLong(long defaultValue) {
    try {
      return value == null ? defaultValue : Long.parseLong(value);
    } catch (NumberFormatException e) {
      if (log.isDebugEnabled()) {
        log.debug("Error parsing long value", e);
      }
      return defaultValue;
    }
  }

  public double asDouble(double defaultValue) {
    try {
      return value == null ? defaultValue : Double.parseDouble(value);
    } catch (NumberFormatException e) {
      if (log.isDebugEnabled()) {
        log.debug("Error parsing double value", e);
      }
      return defaultValue;
    }
  }

  public LocalDateTime asLocalDateTime(LocalDateTime defaultValue) {
    try {
      return value == null ? defaultValue : LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (DateTimeParseException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Error parsing LocalDateTime value", ex);
      }
      return defaultValue;
    }
  }

  private static final class Default extends Property {

    private Default(String key, String value, Map<String, Object> attributes, ObjectMapper mapper) {
      super(key, value, attributes, mapper);
    }
  }
}
