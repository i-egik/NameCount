package ru.pastor.templates.named.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import ru.pastor.templates.named.model.CatalogueModel;
import ru.pastor.templates.named.repository.entity.CatalogueEntity;

/**
 * Интерфейс маппера для преобразования между сущностями каталога и моделями данных.
 * Использует MapStruct для автоматической генерации кода преобразования.
 * Настроен для проверки null значений и игнорирования неотображаемых целевых полей.
 */
@Mapper(
  collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CatalogueMapper {

  /**
   * Экземпляр маппера для статического доступа.
   * Создается автоматически фабрикой MapStruct.
   */
  CatalogueMapper INSTANCE = Mappers.getMapper(CatalogueMapper.class);

  /**
   * Преобразует сущность каталога в модель данных.
   * Отображает поля id и name сущности в поля information.id и information.name модели.
   *
   * @param entity сущность каталога из базы данных
   * @return модель данных каталога для использования в бизнес-логике
   */
  @Mapping(source = "id", target = "information.id")
  @Mapping(source = "name", target = "information.name")
  @Mapping(source = "description", target = "description")
  CatalogueModel toModel(CatalogueEntity entity);

  /**
   * Преобразует модель данных каталога в сущность.
   * Отображает поля information.id и information.name модели в поля id и name сущности.
   *
   * @param model модель данных каталога из бизнес-логики
   * @return сущность каталога для сохранения в базе данных
   */
  @Mapping(source = "information.id", target = "id")
  @Mapping(source = "information.name", target = "name")
  @Mapping(source = "description", target = "description")
  CatalogueEntity toEntity(CatalogueModel model);
}
