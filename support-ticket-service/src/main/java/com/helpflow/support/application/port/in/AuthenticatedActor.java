package com.helpflow.support.application.port.in;

import com.helpflow.support.domain.model.Role;

// Quién hace la petición; lo construye el filtro JWT
public record AuthenticatedActor(Long usuarioId, String correo, Role rol) {
    public boolean esAdmin() { return rol == Role.ROLE_ADMIN; }
    public boolean esTecnico() { return rol == Role.ROLE_TECNICO; }
}
