package com.helpflow.support.infrastructure.adapter.in.rest.mapper;

import com.helpflow.support.application.port.in.RegisterUserCommand;
import com.helpflow.support.domain.model.User;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.RegisterRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper
public interface UserWebMapper {
    RegisterUserCommand toCommand(RegisterRequest request);
    UserResponse toResponse(User user);
}
