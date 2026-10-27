package com.helpflow.support.application.port.in;

import com.helpflow.support.domain.model.User;

public interface UserUseCase {
    User registrar(RegisterUserCommand command);
    LoginResult login(String correo, String password);
    User obtener(Long id, AuthenticatedActor actor);
    User actualizar(Long id, UpdateUserCommand command, AuthenticatedActor actor);
}
