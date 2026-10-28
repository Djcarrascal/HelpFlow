package com.helpflow.support.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpflow.support.application.port.out.TechnicianDispatchPort;
import com.helpflow.support.application.port.out.TechnicianDispatchPort.DispatchSuggestion;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// cron "-" desactiva la tarea programada: el escalamiento solo ocurre cuando la prueba lo pide
@SpringBootTest(properties = "helpflow.escalamiento.cron=-")
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    // Un solo contenedor para todas las clases de prueba (se inicia una vez)
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbc;

    @Value("${helpflow.jwt.secret}")
    private String jwtSecret;

    // Reemplaza el adaptador REST real: no hace falta dispatch encendido
    @MockitoBean protected TechnicianDispatchPort dispatch;

    // Datos limpios por prueba: solo quedan los datos iniciales de V3
    @BeforeEach
    void limpiarDatos() {
        jdbc.update("DELETE FROM ticket_historial");
        jdbc.update("DELETE FROM tickets");
        jdbc.update("DELETE FROM usuarios WHERE rol = 'ROLE_USUARIO' "
                + "AND correo NOT IN ('ana@helpflow.com', 'luis@helpflow.com')");
        jdbc.update("UPDATE usuarios SET estado = 'ACTIVO'");
        jdbc.update("UPDATE tecnicos SET estado = CASE WHEN codigo = 'TEC-004' THEN 'INACTIVO' ELSE 'ACTIVO' END");
        jdbc.update("UPDATE sla_politicas SET horas_limite = CASE prioridad "
                + "WHEN 'CRITICA' THEN 4 WHEN 'ALTA' THEN 8 WHEN 'MEDIA' THEN 24 ELSE 72 END");
    }

    @BeforeEach
    void dispatchPorDefecto() {
        dispatchSugiere("TEC-009", 2);
    }

    // willReturn(...).given(...) no invoca el mock al configurarlo (seguro aunque antes lanzara una excepción)
    protected void dispatchSugiere(String tecnico, int horas) {
        willReturn(new DispatchSuggestion(tecnico, "ALTA", horas)).given(dispatch).sugerir(anyLong(), any(), any());
    }

    protected String login(String correo) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"Password123*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    protected JsonNode crearTicketRed(String token) throws Exception {
        return crearTicket(token, "RED", "ALTA");
    }

    protected JsonNode crearTicket(String token, String categoria, String prioridad) throws Exception {
        String json = """
                {"titulo":"Sin acceso a la VPN","descripcion":"La VPN rechaza la conexión",
                 "categoria":"%s","prioridad":"%s","sede":"BARRANQUILLA","activoAfectado":"ACT-1032"}
                """.formatted(categoria, prioridad);
        String body = mvc.perform(post("/api/tickets").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    protected ResultActions cambiarEstado(long id, String token, String json) throws Exception {
        return mvc.perform(patch("/api/tickets/{id}/estado", id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected JsonNode historial(long id, String token) throws Exception {
        String body = mvc.perform(get("/api/tickets/{id}/historial", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    // Mueve las fechas de un ticket para simular el paso del tiempo
    protected void moverFechas(long ticketId, LocalDateTime creacion, LocalDateTime limiteSla) {
        jdbc.update("UPDATE tickets SET fecha_creacion = ?, fecha_limite_sla = ?, escalado = false WHERE id = ?",
                Timestamp.valueOf(creacion), Timestamp.valueOf(limiteSla), ticketId);
    }

    // Token bien firmado con la clave de la aplicación pero vencido hace una hora
    protected String tokenExpirado() {
        Instant hace2h = Instant.now().minus(2, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject("ana@helpflow.com")
                .claim("uid", 2)
                .claim("rol", "ROLE_USUARIO")
                .issuedAt(Date.from(hace2h))
                .expiration(Date.from(hace2h.plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    // TEC-009 -> tec009@helpflow.com (convención de los datos de V3)
    protected static String correoDe(String codigoTecnico) {
        return "tec" + codigoTecnico.substring(4) + "@helpflow.com";
    }
}
