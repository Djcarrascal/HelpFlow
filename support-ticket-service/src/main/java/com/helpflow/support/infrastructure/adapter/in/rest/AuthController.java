package com.helpflow.support.infrastructure.adapter.in.rest;

import com.helpflow.support.application.port.in.LoginResult;
import com.helpflow.support.application.port.in.UserUseCase;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.LoginRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.RegisterRequest;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.UserResponse;
import com.helpflow.support.infrastructure.adapter.in.rest.mapper.UserWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Registro e inicio de sesión con JWT")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserUseCase userUseCase;
    private final UserWebMapper mapper;

    public AuthController(UserUseCase userUseCase, UserWebMapper mapper) {
        this.userUseCase = userUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Registrar usuario", description = "Crea un usuario ACTIVO con rol ROLE_USUARIO")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return mapper.toResponse(userUseCase.registrar(mapper.toCommand(request)));
    }

    @Operation(summary = "Iniciar sesión", description = "Devuelve un token Bearer JWT")
    @PostMapping("/login")
    public LoginResult login(@Valid @RequestBody LoginRequest request) {
        return userUseCase.login(request.correo(), request.password());
    }
}
