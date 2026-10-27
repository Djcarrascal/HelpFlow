package com.helpflow.support.infrastructure.adapter.in.rest;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.in.UserUseCase;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.UpdateUserRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.UserResponse;
import com.helpflow.support.infrastructure.adapter.in.rest.mapper.TicketWebMapper;
import com.helpflow.support.infrastructure.adapter.in.rest.mapper.UserWebMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuarios", description = "Perfil e información básica")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserUseCase userUseCase;
    private final UserWebMapper userMapper;
    private final TicketWebMapper commandMapper;

    public UserController(UserUseCase userUseCase, UserWebMapper userMapper, TicketWebMapper commandMapper) {
        this.userUseCase = userUseCase;
        this.userMapper = userMapper;
        this.commandMapper = commandMapper;
    }

    @GetMapping("/me")
    public UserResponse yo(@AuthenticationPrincipal AuthenticatedActor actor) {
        return userMapper.toResponse(userUseCase.obtener(actor.usuarioId(), actor));
    }

    @GetMapping("/{id}")
    public UserResponse obtener(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedActor actor) {
        return userMapper.toResponse(userUseCase.obtener(id, actor));
    }

    // Editar información básica y perfil de soporte
    @PatchMapping("/{id}")
    public UserResponse actualizar(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                                   @AuthenticationPrincipal AuthenticatedActor actor) {
        return userMapper.toResponse(userUseCase.actualizar(id, commandMapper.toCommand(request), actor));
    }
}
