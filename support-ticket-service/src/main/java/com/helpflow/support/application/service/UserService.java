package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.in.LoginResult;
import com.helpflow.support.application.port.in.RegisterUserCommand;
import com.helpflow.support.application.port.in.UpdateUserCommand;
import com.helpflow.support.application.port.in.UserUseCase;
import com.helpflow.support.application.port.out.PasswordHasherPort;
import com.helpflow.support.application.port.out.TokenPort;
import com.helpflow.support.application.port.out.UserRepositoryPort;
import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.DuplicateResourceException;
import com.helpflow.support.domain.exception.ForbiddenOperationException;
import com.helpflow.support.domain.exception.InvalidCredentialsException;
import com.helpflow.support.domain.exception.ResourceNotFoundException;
import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.domain.model.Role;
import com.helpflow.support.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService implements UserUseCase {

    private final UserRepositoryPort usuarios;
    private final PasswordHasherPort hasher;
    private final TokenPort tokens;

    public UserService(UserRepositoryPort usuarios, PasswordHasherPort hasher, TokenPort tokens) {
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    // Registro público: siempre ROLE_USUARIO y ACTIVO
    @Override
    public User registrar(RegisterUserCommand cmd) {
        if (usuarios.existsByDocumento(cmd.documento())) {
            throw new DuplicateResourceException("Documento ya registrado");
        }
        if (usuarios.existsByCorreo(cmd.correo().toLowerCase())) {
            throw new DuplicateResourceException("Correo ya registrado");
        }
        User u = new User();
        u.setDocumento(cmd.documento());
        u.setNombre(cmd.nombre());
        u.setCorreo(cmd.correo().toLowerCase());
        u.setPasswordHash(hasher.hash(cmd.password()));
        u.setSede(cmd.sede());
        u.setArea(cmd.area());
        u.setEstado(RecordStatus.ACTIVO);
        u.setRol(Role.ROLE_USUARIO);
        return usuarios.save(u);
    }

    // Mismo mensaje para correo o contraseña incorrectos: no revela cuál falló
    @Override
    @Transactional(readOnly = true)
    public LoginResult login(String correo, String password) {
        User u = usuarios.findByCorreo(correo.toLowerCase())
                .filter(user -> hasher.matches(password, user.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!u.estaActivo()) {
            throw new BusinessRuleException("Usuario inactivo");
        }
        return new LoginResult(tokens.generar(u), "Bearer", tokens.expiracionSegundos(), u.getRol().name());
    }

    @Override
    @Transactional(readOnly = true)
    public User obtener(Long id, AuthenticatedActor actor) {
        verificarPropioOAdmin(id, actor);
        return usuarios.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    @Override
    public User actualizar(Long id, UpdateUserCommand cmd, AuthenticatedActor actor) {
        User u = obtener(id, actor);
        if (cmd.nombre() != null) u.setNombre(cmd.nombre());
        if (cmd.sede() != null) u.setSede(cmd.sede());
        if (cmd.area() != null) u.setArea(cmd.area());
        if (cmd.estado() != null) {
            if (!actor.esAdmin()) throw new ForbiddenOperationException("Solo un admin cambia el estado");
            u.setEstado(cmd.estado());
        }
        return usuarios.save(u);
    }

    private void verificarPropioOAdmin(Long id, AuthenticatedActor actor) {
        if (!actor.esAdmin() && !actor.usuarioId().equals(id)) {
            throw new ForbiddenOperationException("Solo puedes ver o editar tu propio perfil");
        }
    }
}
