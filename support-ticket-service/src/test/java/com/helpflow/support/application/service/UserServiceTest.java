package com.helpflow.support.application.service;

import com.helpflow.support.application.port.in.AuthenticatedActor;
import com.helpflow.support.application.port.in.LoginResult;
import com.helpflow.support.application.port.in.RegisterUserCommand;
import com.helpflow.support.application.port.in.UpdateUserCommand;
import com.helpflow.support.application.port.out.PasswordHasherPort;
import com.helpflow.support.application.port.out.TokenPort;
import com.helpflow.support.application.port.out.UserRepositoryPort;
import com.helpflow.support.domain.exception.BusinessRuleException;
import com.helpflow.support.domain.exception.DuplicateResourceException;
import com.helpflow.support.domain.exception.ForbiddenOperationException;
import com.helpflow.support.domain.exception.InvalidCredentialsException;
import com.helpflow.support.domain.model.RecordStatus;
import com.helpflow.support.domain.model.Role;
import com.helpflow.support.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final RegisterUserCommand REGISTRO = new RegisterUserCommand(
            "55667788", "Marta Ruiz", "Marta@HelpFlow.com", "Segura2026*", "MEDELLIN", "COMPRAS");
    private static final AuthenticatedActor ADMIN = new AuthenticatedActor(1L, "admin@helpflow.com", Role.ROLE_ADMIN);
    private static final AuthenticatedActor ANA = new AuthenticatedActor(2L, "ana@helpflow.com", Role.ROLE_USUARIO);

    @Mock UserRepositoryPort usuarios;
    @Mock PasswordHasherPort hasher;
    @Mock TokenPort tokens;

    @Captor ArgumentCaptor<User> userCaptor;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(usuarios, hasher, tokens);
    }

    private User ana(RecordStatus estado) {
        User u = new User();
        u.setId(2L);
        u.setCorreo("ana@helpflow.com");
        u.setPasswordHash("HASH");
        u.setNombre("Ana");
        u.setSede("BARRANQUILLA");
        u.setRol(Role.ROLE_USUARIO);
        u.setEstado(estado);
        return u;
    }

    @Test
    void registrarCreaUsuarioActivoConRolUsuarioYClaveHasheada() {
        when(hasher.hash("Segura2026*")).thenReturn("HASH");
        when(usuarios.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrar(REGISTRO);

        verify(usuarios).save(userCaptor.capture());
        User guardado = userCaptor.getValue();
        assertThat(guardado.getCorreo()).isEqualTo("marta@helpflow.com");     // normalizado
        assertThat(guardado.getPasswordHash()).isEqualTo("HASH");             // nunca la clave en texto plano
        assertThat(guardado.getRol()).isEqualTo(Role.ROLE_USUARIO);
        assertThat(guardado.getEstado()).isEqualTo(RecordStatus.ACTIVO);
    }

    @Test
    void documentoDuplicado() {
        when(usuarios.existsByDocumento("55667788")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(REGISTRO))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("Documento");
        verify(usuarios, never()).save(any());
    }

    @Test
    void correoDuplicadoSinImportarMayusculas() {
        when(usuarios.existsByCorreo("marta@helpflow.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(REGISTRO))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("Correo");
    }

    @Test
    void loginCorrectoDevuelveTokenBearer() {
        when(usuarios.findByCorreo("ana@helpflow.com")).thenReturn(Optional.of(ana(RecordStatus.ACTIVO)));
        when(hasher.matches("Password123*", "HASH")).thenReturn(true);
        when(tokens.generar(any(User.class))).thenReturn("jwt");
        when(tokens.expiracionSegundos()).thenReturn(3600L);

        LoginResult r = service.login("ANA@helpflow.com", "Password123*");

        assertThat(r.token()).isEqualTo("jwt");
        assertThat(r.tipo()).isEqualTo("Bearer");
        assertThat(r.expiraEnSegundos()).isEqualTo(3600L);
        assertThat(r.rol()).isEqualTo("ROLE_USUARIO");
    }

    @Test
    void claveIncorrectaNoGeneraToken() {
        when(usuarios.findByCorreo("ana@helpflow.com")).thenReturn(Optional.of(ana(RecordStatus.ACTIVO)));
        when(hasher.matches("mala", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login("ana@helpflow.com", "mala")).isInstanceOf(InvalidCredentialsException.class);
        verify(tokens, never()).generar(any());
    }

    @Test
    void correoInexistenteDaElMismoErrorQueClaveIncorrecta() {
        when(usuarios.findByCorreo("nadie@helpflow.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("nadie@helpflow.com", "x")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() {
        when(usuarios.findByCorreo("ana@helpflow.com")).thenReturn(Optional.of(ana(RecordStatus.INACTIVO)));
        when(hasher.matches("Password123*", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> service.login("ana@helpflow.com", "Password123*")).isInstanceOf(BusinessRuleException.class);
        verify(tokens, never()).generar(any());
    }

    @Test
    void unUsuarioNoPuedeVerOtroPerfil() {
        assertThatThrownBy(() -> service.obtener(3L, ANA)).isInstanceOf(ForbiddenOperationException.class);
        verify(usuarios, never()).findById(any());
    }

    @Test
    void elUsuarioEditaSoloLosCamposQueEnvia() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(ana(RecordStatus.ACTIVO)));
        when(usuarios.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User r = service.actualizar(2L, new UpdateUserCommand(null, null, "CONTABILIDAD", null), ANA);

        assertThat(r.getArea()).isEqualTo("CONTABILIDAD");
        assertThat(r.getNombre()).isEqualTo("Ana");              // no se envió: no cambia
        assertThat(r.getSede()).isEqualTo("BARRANQUILLA");
    }

    @Test
    void elUsuarioNoPuedeCambiarSuEstado() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(ana(RecordStatus.ACTIVO)));

        assertThatThrownBy(() -> service.actualizar(2L, new UpdateUserCommand(null, null, null, RecordStatus.INACTIVO), ANA))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(usuarios, never()).save(any());
    }

    @Test
    void elAdminPuedeDesactivarUsuarios() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(ana(RecordStatus.ACTIVO)));
        when(usuarios.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User r = service.actualizar(2L, new UpdateUserCommand("Ana María", "MEDELLIN", null, RecordStatus.INACTIVO), ADMIN);

        assertThat(r.getEstado()).isEqualTo(RecordStatus.INACTIVO);
        assertThat(r.getNombre()).isEqualTo("Ana María");
        assertThat(r.getSede()).isEqualTo("MEDELLIN");
    }
}
