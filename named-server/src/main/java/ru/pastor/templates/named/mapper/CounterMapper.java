package ru.pastor.templates.named.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import ru.pastor.templates.named.model.CounterModel;
import ru.pastor.templates.named.repository.entity.CounterEntity;

@Mapper(
  collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CounterMapper {

  CounterMapper INSTANCE = Mappers.getMapper(CounterMapper.class);
  
  @Mapping(source = "id", target = "id")
  @Mapping(source = "catalogue.id", target = "information.id")
  @Mapping(source = "catalogue.name", target = "information.name")
  @Mapping(source = "userId", target = "userId")
  @Mapping(source = "value", target = "value")
  CounterModel toModel(CounterEntity entity);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "information.id", target = "catalogue.id")
  @Mapping(source = "information.name", target = "catalogue.name")
  @Mapping(source = "userId", target = "userId")
  @Mapping(source = "value", target = "value")
  CounterEntity toEntity(CounterModel model);

}
