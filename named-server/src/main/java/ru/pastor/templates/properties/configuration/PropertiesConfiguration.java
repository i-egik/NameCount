package ru.pastor.templates.properties.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import ru.pastor.templates.properties.Property;
import ru.pastor.templates.properties.repository.PropertyRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
public class PropertiesConfiguration {

  @Value("${app.properties.cached:true}")
  private boolean cached;
  @Value("${app.properties.cached-expires:360s}")
  private Duration cachedExpires;

  @Bean("site")
  public PropertyRepository repositorySite(MeterRegistry registry, DatabaseClient client, TransactionalOperator tx, ObjectMapper mapper) {
    PropertyRepository.Standard repository = new PropertyRepository.Standard(
      client, tx, (row, metadata) -> createProperty(mapper, row, metadata),
      page -> "SELECT key, value, expires, refresh " +
        "FROM properties.site WHERE id >= :offset ORDER BY id LIMIT :limit",
      property -> "INSERT INTO properties.site(key, value, expires, refresh) " +
        "VALUES (:key, :value, :expires, :refresh) ON CONFLICT(key) " +
        "DO UPDATE SET value = :value, expires = :expires, refresh = :refresh",
      key -> "SELECT key, value, expires, refresh " +
        "FROM properties.site WHERE key = :key",
      key -> "DELETE FROM properties.site WHERE key = :key");
    return cached ? new PropertyRepository.Cached("site", cachedExpires, registry, repository) : repository;
  }

  private static Property createProperty(ObjectMapper mapper, Row row, RowMetadata metadata) {
    String key = row.get("key", String.class);
    String value = row.get("value", String.class);
    Map<String, Object> attributes = new HashMap<>();
    metadata.getColumnMetadatas().forEach(cm -> {
      String columnName = cm.getName();
      if ("key".equals(columnName) || "value".equals(columnName)) {
        return;
      }
      Class<?> type = Objects.requireNonNull(cm.getJavaType());
      if (type.isAssignableFrom(String.class)) {
        attributes.put(columnName, row.get(columnName, String.class));
      } else if (type.isAssignableFrom(Integer.class)) {
        attributes.put(columnName, row.get(columnName, Integer.class));
      } else if (type.isAssignableFrom(Long.class)) {
        attributes.put(columnName, row.get(columnName, Long.class));
      } else if (type.isAssignableFrom(Double.class)) {
        attributes.put(columnName, row.get(columnName, Double.class));
      } else if (type.isAssignableFrom(LocalDateTime.class)) {
        attributes.put(columnName, row.get(columnName, LocalDateTime.class));
      } else {
        attributes.put(columnName, row.get(columnName, Object.class));
      }
    });
    return Property.of(key, value, mapper, attributes);
  }
}
