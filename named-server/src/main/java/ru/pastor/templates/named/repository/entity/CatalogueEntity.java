package ru.pastor.templates.named.repository.entity;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record CatalogueEntity(
  Long id,
  String name,
  String description,
  LocalDateTime created,
  LocalDateTime updated
) {
}
