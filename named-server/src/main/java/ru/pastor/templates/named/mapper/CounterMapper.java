package ru.pastor.templates.named.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import ru.pastor.templates.named.model.CounterModel;
import ru.pastor.templates.named.repository.entity.CounterEntity;

/**
 * Интерфейс маппера для преобразования между сущностями счетчиков и моделями данных.
 * Использует MapStruct для автоматической генерации кода преобразования.
 * Настроен для проверки null значений и игнорирования неотображаемых целевых полей.
 * Обрабатывает сложную структуру объектов с вложенными полями.
 */
@Mapper(
  collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CounterMapper {

  /**
   * Экземпляр маппера для статического доступа.
   * Создается автоматически фабрикой MapStruct.
   */
  CounterMapper INSTANCE = Mappers.getMapper(CounterMapper.class);

  /**
   * Преобразует сущность счетчика в модель данных.
   * Отображает поля сущности в соответствующие поля модели,
   * включая вложенные поля catalogue.id и catalogue.name в information.id и information.name.
   *
   * @param entity сущность счетчика из базы данных
   * @return модель данных счетчика для использования в бизнес-логике
   */
  @Mapping(source = "id", target = "id")
  @Mapping(source = "catalogue.id", target = "information.id")
  @Mapping(source = "catalogue.name", target = "information.name")
  @Mapping(source = "userId", target = "userId")
  @Mapping(source = "value", target = "value")
  CounterModel toModel(CounterEntity entity);

  /**
   * Преобразует модель данных счетчика в сущность.
   * Отображает поля модели в соответствующие поля сущности,
   * включая вложенные поля information.id и information.name в catalogue.id и catalogue.name.
   *
   * @param model модель данных счетчика из бизнес-логики
   * @return сущность счетчика для сохранения в базе данных
   */
  @Mapping(source = "id", target = "id")
  @Mapping(source = "information.id", target = "catalogue.id")
  @Mapping(source = "information.name", target = "catalogue.name")
  @Mapping(source = "userId", target = "userId")
  @Mapping(source = "value", target = "value")
  CounterEntity toEntity(CounterModel model);

}
