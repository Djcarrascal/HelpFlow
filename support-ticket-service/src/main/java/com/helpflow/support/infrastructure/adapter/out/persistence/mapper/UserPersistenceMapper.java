package com.helpflow.support.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.support.domain.model.User;
import com.helpflow.support.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserPersistenceMapper {
    User toDomain(UserEntity entity);

    @Mapping(target = "tickets", ignore = true)
    UserEntity toEntity(User user);
}
