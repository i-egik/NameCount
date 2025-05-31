package ru.pastor.templates.named.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder(toBuilder = true)
public record CounterModel(Long id,
                           @NotNull CatalogueModel.Information information,
                           @NotNull Long userId,
                           @Positive long value) {
}
