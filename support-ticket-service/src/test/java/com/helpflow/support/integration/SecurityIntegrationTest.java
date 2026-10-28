package com.helpflow.support.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Autenticación, expiración de tokens y control de acceso por rol
class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String ERRORES = "https://api.helpflow.com/errors/";

    @Test
    void elLoginDevuelveUnTokenBearerConExpiracion() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@helpflow.com\",\"password\":\"Password123*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andExpect(jsonPath("$.rol").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void sinTokenResponde401NoAutenticado() throws Exception {
        mvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.type").value(ERRORES + "no-autenticado"))
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unTokenExpiradoSeDistingueDeUnoInvalido() throws Exception {
        mvc.perform(get("/api/tickets").header("Authorization", bearer(tokenExpirado())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ERRORES + "token-expirado"))
                .andExpect(jsonPath("$.detail").value("El token expiró; inicia sesión de nuevo"));
    }

    @Test
    void unTokenFirmadoConOtraClaveEsInvalido() throws Exception {
        String falso = Jwts.builder()
                .subject("ana@helpflow.com").claim("uid", 2).claim("rol", "ROLE_ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("b3RyYS1jbGF2ZS1kZS1wcnVlYmEtZGUtMzItYnl0ZXM=")))
                .compact();

        mvc.perform(get("/api/tickets").header("Authorization", bearer(falso)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ERRORES + "token-invalido"));
    }

    @Test
    void unTokenMalFormadoEsInvalido() throws Exception {
        mvc.perform(get("/api/tickets").header("Authorization", "Bearer esto.no.es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ERRORES + "token-invalido"));
    }

    @Test
    void credencialesIncorrectas() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@helpflow.com\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ERRORES + "credenciales-invalidas"));
    }

    @Test
    void unUsuarioInactivoNoPuedeIniciarSesion() throws Exception {
        jdbc.update("UPDATE usuarios SET estado = 'INACTIVO' WHERE correo = 'luis@helpflow.com'");

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"luis@helpflow.com\",\"password\":\"Password123*\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void lasMetricasSonSoloParaAdmin() throws Exception {
        mvc.perform(get("/actuator/metrics").header("Authorization", bearer(login("ana@helpflow.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(ERRORES + "acceso-denegado"));
        // En pruebas Spring Boot no exporta a Prometheus; /actuator/metrics usa el registro en memoria
        mvc.perform(get("/actuator/metrics").header("Authorization", bearer(login("admin@helpflow.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void laConfiguracionDeSlaEsSoloParaAdmin() throws Exception {
        mvc.perform(get("/api/sla").header("Authorization", bearer(login("ana@helpflow.com"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/sla").header("Authorization", bearer(login("tec009@helpflow.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elTecnicoNoPuedeCrearTickets() throws Exception {
        mvc.perform(post("/api/tickets").header("Authorization", bearer(login("tec009@helpflow.com")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthInfoYSwaggerSonPublicos() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void lasFallasDeAutenticacionSeRegistranEnMetricas() throws Exception {
        mvc.perform(get("/api/tickets").header("Authorization", bearer(tokenExpirado())));

        mvc.perform(get("/actuator/metrics/helpflow.auth.fallos").header("Authorization", bearer(login("admin@helpflow.com")))
                        .param("tag", "motivo:token-expirado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value").isNumber());
    }
}
