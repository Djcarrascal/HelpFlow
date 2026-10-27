package com.helpflow.support.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpflow.support.application.port.out.TechnicianDispatchPort;
import com.helpflow.support.application.port.out.TechnicianDispatchPort.DispatchSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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

    // Reemplaza el adaptador REST real: no hace falta dispatch encendido
    @MockitoBean protected TechnicianDispatchPort dispatch;

    @BeforeEach
    void dispatchPorDefecto() {
        given(dispatch.sugerir(anyLong(), any(), any())).willReturn(new DispatchSuggestion("TEC-009", "ALTA", 2));
    }

    protected String login(String correo) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"Password123*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    protected JsonNode crearTicketRed(String token) throws Exception {
        String json = """
                {"titulo":"Sin acceso a la VPN","descripcion":"La VPN rechaza la conexión",
                 "categoria":"RED","prioridad":"ALTA","sede":"BARRANQUILLA","activoAfectado":"ACT-1032"}
                """;
        String body = mvc.perform(post("/api/tickets").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    // TEC-009 -> tec009@helpflow.com (convención de los datos de V3)
    protected static String correoDe(String codigoTecnico) {
        return "tec" + codigoTecnico.substring(4) + "@helpflow.com";
    }
}
