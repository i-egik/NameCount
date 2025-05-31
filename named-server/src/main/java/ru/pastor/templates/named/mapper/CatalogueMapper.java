package ru.pastor.templates.named.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import ru.pastor.templates.named.model.CatalogueModel;
import ru.pastor.templates.named.repository.entity.CatalogueEntity;

@Mapper(
  collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CatalogueMapper {

  CatalogueMapper INSTANCE = Mappers.getMapper(CatalogueMapper.class);

  @Mapping(source = "id", target = "information.id")
  @Mapping(source = "name", target = "information.name")
  @Mapping(source = "description", target = "description")
  CatalogueModel toModel(CatalogueEntity entity);

  @Mapping(source = "information.id", target = "id")
  @Mapping(source = "information.name", target = "name")
  @Mapping(source = "description", target = "description")
  CatalogueEntity toEntity(CatalogueModel model);
}
