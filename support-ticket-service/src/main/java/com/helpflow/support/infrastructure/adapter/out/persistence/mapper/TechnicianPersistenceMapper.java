package com.helpflow.support.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.support.domain.model.Technician;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.TechnicianEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TechnicianPersistenceMapper {
    // Leer usuario.id de un proxy LAZY no dispara consulta
    @Mapping(target = "usuarioId", source = "usuario.id")
    Technician toDomain(TechnicianEntity entity);
}
