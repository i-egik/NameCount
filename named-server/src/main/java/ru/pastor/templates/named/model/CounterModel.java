package ru.pastor.templates.named.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

/**
 * Модель данных для представления счетчика.
 * Используется для передачи информации о счетчике между слоями приложения.
 * Реализована как неизменяемая запись (record) с возможностью создания через Builder.
 */
@Builder(toBuilder = true)
public record CounterModel(
  /**
   * Уникальный идентификатор счетчика.
   * Может быть null для новых счетчиков.
   */
  Long id,

  /**
   * Информация о счетчике из каталога.
   * Не может быть null.
   */
  @NotNull CatalogueModel.Information information,

  /**
   * Идентификатор пользователя, которому принадлежит счетчик.
   * Не может быть null.
   */
  @NotNull Long userId,

  /**
   * Текущее значение счетчика.
   * Должно быть положительным числом.
   */
  @Positive long value) {
}
