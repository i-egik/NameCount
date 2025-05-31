package ru.pastor.templates.named.repository.entity;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record CounterEntity(
  Long id,
  CatalogueEntity catalogue,
  long userId,
  long value,
  LocalDateTime created,
  LocalDateTime updated
) {
}
