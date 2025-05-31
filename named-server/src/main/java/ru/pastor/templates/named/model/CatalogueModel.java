package ru.pastor.templates.named.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder(toBuilder = true)
public record CatalogueModel(@NotNull Information information, String description) {
  @SuppressWarnings("UnnecessaryModifier")
  @Builder(toBuilder = true)
  public static record Information(Long id, @NotBlank String name) {

  }
}
