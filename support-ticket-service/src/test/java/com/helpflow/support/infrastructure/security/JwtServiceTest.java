package com.helpflow.support.infrastructure.security;

import com.helpflow.support.domain.model.Role;
import com.helpflow.support.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Clave de prueba de 256 bits en Base64 (no es la de la aplicación)
    private static final String CLAVE = "2ylE+OnRx31qbB+XB12P0onoklSq+WbxeLT97emXKww=";
    private static final String OTRA_CLAVE = "b3RyYS1jbGF2ZS1kZS1wcnVlYmEtZGUtMzItYnl0ZXM=";

    private User ana() {
        User u = new User();
        u.setId(2L);
        u.setCorreo("ana@helpflow.com");
        u.setRol(Role.ROLE_USUARIO);
        return u;
    }

    @Test
    void elTokenGeneradoContieneCorreoIdYRol() {
        JwtService jwt = new JwtService(CLAVE, 60);

        Claims claims = jwt.validar(jwt.generar(ana()));

        assertThat(claims.getSubject()).isEqualTo("ana@helpflow.com");
        assertThat(((Number) claims.get("uid")).longValue()).isEqualTo(2L);
        assertThat(claims.get("rol", String.class)).isEqualTo("ROLE_USUARIO");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(jwt.expiracionSegundos()).isEqualTo(3600L);
    }

    @Test
    void unTokenVencidoSeRechazaComoExpirado() {
        // Expiración negativa: el token nace vencido
        JwtService jwtVencido = new JwtService(CLAVE, -1);
        String token = jwtVencido.generar(ana());

        assertThatThrownBy(() -> new JwtService(CLAVE, 60).validar(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        String token = new JwtService(OTRA_CLAVE, 60).generar(ana());

        assertThatThrownBy(() -> new JwtService(CLAVE, 60).validar(token))
                .isInstanceOf(JwtException.class)
                .isNotInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void unTokenConElContenidoAlteradoSeRechaza() {
        JwtService jwt = new JwtService(CLAVE, 60);
        String[] partes = jwt.generar(ana()).split("\\.");
        // Alguien cambia el payload para hacerse admin pero conserva la firma original
        String payloadFalso = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"ana@helpflow.com\",\"uid\":2,\"rol\":\"ROLE_ADMIN\"}".getBytes(StandardCharsets.UTF_8));
        String alterado = partes[0] + "." + payloadFalso + "." + partes[2];

        assertThatThrownBy(() -> jwt.validar(alterado)).isInstanceOf(JwtException.class);
    }
}
