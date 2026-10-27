package com.helpflow.support.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.support.domain.model.SlaPolicy;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.SlaPolicyEntity;
import org.mapstruct.Mapper;

@Mapper
public interface SlaPolicyPersistenceMapper {
    SlaPolicy toDomain(SlaPolicyEntity entity);
    SlaPolicyEntity toEntity(SlaPolicy policy);
}
