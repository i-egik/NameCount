package ru.pastor.templates.named.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Модель данных для представления элемента каталога счетчиков.
 * Содержит основную информацию о счетчике и его описание.
 * Реализована как неизменяемая запись (record) с возможностью создания через Builder.
 */
@Builder(toBuilder = true)
public record CatalogueModel(
    /**
     * Основная информация о счетчике (идентификатор и имя).
     * Не может быть null.
     */
    @NotNull Information information,

    /**
     * Описание счетчика.
     * Может быть null.
     */
    String description) {

  /**
   * Внутренняя модель данных для хранения основной информации о счетчике.
   * Содержит идентификатор и имя счетчика.
   * Реализована как неизменяемая статическая запись (record) с возможностью создания через Builder.
   */
  @SuppressWarnings("UnnecessaryModifier")
  @Builder(toBuilder = true)
  public static record Information(
      /**
       * Уникальный идентификатор счетчика в каталоге.
       * Может быть null для новых элементов каталога.
       */
      Long id,

      /**
       * Имя счетчика.
       * Не может быть пустым или null.
       */
      @NotBlank String name) {
  }
}
