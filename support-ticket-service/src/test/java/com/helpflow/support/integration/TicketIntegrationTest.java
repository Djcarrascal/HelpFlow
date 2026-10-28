package com.helpflow.support.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpflow.support.application.port.out.DispatchUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketIntegrationTest extends AbstractIntegrationTest {

    private static final String ERRORES = "https://api.helpflow.com/errors/";

    // ---------- end-to-end: login -> solicitud -> evaluación ----------

    @Test
    void flujoCompletoDesdeElLoginHastaElCierre() throws Exception {
        // 1. El usuario inicia sesión y registra el ticket
        String ana = login("ana@helpflow.com");
        JsonNode ticket = crearTicketRed(ana);
        long id = ticket.get("id").asLong();

        // 2. Queda asignado al técnico sugerido, con SLA de 8 h para prioridad ALTA
        assertThat(ticket.get("estado").asText()).isEqualTo("ASIGNADO");
        assertThat(ticket.get("tecnicoCodigo").asText()).isEqualTo("TEC-009");
        LocalDateTime creacion = LocalDateTime.parse(ticket.get("fechaCreacion").asText());
        LocalDateTime limite = LocalDateTime.parse(ticket.get("fechaLimiteSla").asText());
        assertThat(Duration.between(creacion, limite)).isEqualTo(Duration.ofHours(8));

        // 3. El técnico asignado lo gestiona hasta cerrarlo
        String tecnico = login("tec009@helpflow.com");
        cambiarEstado(id, tecnico, "{\"estado\":\"EN_PROGRESO\"}").andExpect(status().isOk());
        cambiarEstado(id, tecnico, "{\"estado\":\"RESUELTO\",\"observacion\":\"Se reinició el túnel\"}")
                .andExpect(jsonPath("$.fechaResolucion").exists());
        cambiarEstado(id, tecnico, "{\"estado\":\"CERRADO\"}").andExpect(jsonPath("$.estado").value("CERRADO"));

        // 4. El historial deja la trazabilidad completa
        JsonNode h = historial(id, ana);
        assertThat(h).hasSize(5);
        assertThat(h.get(0).get("accion").asText()).isEqualTo("CREADO");
        assertThat(h.get(1).get("accion").asText()).isEqualTo("ASIGNADO");
        assertThat(h.get(4).get("estadoNuevo").asText()).isEqualTo("CERRADO");
        assertThat(h.get(4).get("responsable").asText()).isEqualTo("tec009@helpflow.com");

        // 5. El admin ve la métrica de resolución registrada
        mvc.perform(get("/actuator/metrics/helpflow.tickets.resolucion")
                        .header("Authorization", bearer(login("admin@helpflow.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].statistic").value("COUNT"));
    }

    // ---------- transiciones y validaciones ----------

    @Test
    void tecnicoAsignadoGestionaTransicionesValidas() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();
        String tecnico = login("tec009@helpflow.com");

        cambiarEstado(id, tecnico, "{\"estado\":\"EN_PROGRESO\"}").andExpect(status().isOk());
        // validación cruzada: RESUELTO exige observación
        cambiarEstado(id, tecnico, "{\"estado\":\"RESUELTO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.observacion").exists());
        cambiarEstado(id, tecnico, "{\"estado\":\"RESUELTO\",\"observacion\":\"Listo\"}").andExpect(status().isOk());
        // transición fuera de la máquina de estados
        cambiarEstado(id, tecnico, "{\"estado\":\"CANCELADO\",\"observacion\":\"x\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ERRORES + "transicion-invalida"));
    }

    @Test
    void asignadoNoSePuedePedirManualmente() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();

        cambiarEstado(id, login("admin@helpflow.com"), "{\"estado\":\"ASIGNADO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.estado").exists());
    }

    // ---------- control de acceso por propiedad ----------

    @Test
    void cadaRolListaSoloLoQueLeCorresponde() throws Exception {
        String ana = login("ana@helpflow.com");
        String luis = login("luis@helpflow.com");
        crearTicketRed(ana);
        crearTicketRed(ana);
        crearTicketRed(luis);

        listar(login("admin@helpflow.com"), 3);
        listar(ana, 2);
        listar(luis, 1);
        listar(login("tec009@helpflow.com"), 3);     // todos quedaron asignados a TEC-009
        listar(login("tec013@helpflow.com"), 0);     // técnico de ACCESOS: ninguno
    }

    private void listar(String token, int esperados) throws Exception {
        mvc.perform(get("/api/tickets").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(esperados));
    }

    @Test
    void usuarioNoPuedeVerTicketAjeno() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();

        mvc.perform(get("/api/tickets/{id}", id).header("Authorization", bearer(login("luis@helpflow.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(ERRORES + "acceso-denegado"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void tecnicoNoAsignadoNoPuedeGestionar() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();

        // TEC-013 es de ACCESOS: no tiene este ticket
        cambiarEstado(id, login("tec013@helpflow.com"), "{\"estado\":\"EN_PROGRESO\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void elUsuarioNoPuedeCambiarEstados() throws Exception {
        String ana = login("ana@helpflow.com");
        long id = crearTicketRed(ana).get("id").asLong();

        cambiarEstado(id, ana, "{\"estado\":\"CANCELADO\",\"observacion\":\"ya no lo necesito\"}")
                .andExpect(status().isForbidden());
    }

    // ---------- asignación: dispatch caído, sin técnicos, reasignación ----------

    @Test
    void siDispatchFallaSeAsignaUnTecnicoAlternativo() throws Exception {
        willThrow(new DispatchUnavailableException("caído", null)).given(dispatch).sugerir(anyLong(), any(), any());
        String ana = login("ana@helpflow.com");

        JsonNode ticket = crearTicketRed(ana);

        assertThat(ticket.get("estado").asText()).isEqualTo("ASIGNADO");
        assertThat(ticket.get("tecnicoCodigo").asText()).isIn("TEC-009", "TEC-010", "TEC-017", "TEC-018");
        assertThat(historial(ticket.get("id").asLong(), ana).get(1).get("observacion").asText())
                .contains("no disponible");
    }

    @Test
    void sinTecnicosDisponiblesQuedaAbiertoYElAdminReintenta() throws Exception {
        willThrow(new DispatchUnavailableException("caído", null)).given(dispatch).sugerir(anyLong(), any(), any());
        jdbc.update("UPDATE tecnicos SET estado = 'INACTIVO' WHERE sede = 'BARRANQUILLA' AND especialidad IN ('RED', 'OTRO')");
        String ana = login("ana@helpflow.com");

        JsonNode ticket = crearTicketRed(ana);
        long id = ticket.get("id").asLong();
        assertThat(ticket.get("estado").asText()).isEqualTo("ABIERTO");
        assertThat(historial(id, ana).get(1).get("accion").asText()).isEqualTo("SIN_TECNICO");

        // Vuelven los técnicos y dispatch: el admin reintenta la asignación
        jdbc.update("UPDATE tecnicos SET estado = 'ACTIVO' WHERE codigo <> 'TEC-004'");
        dispatchSugiere("TEC-009", 2);
        mvc.perform(post("/api/tickets/{id}/asignar", id).header("Authorization", bearer(login("admin@helpflow.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ASIGNADO"))
                .andExpect(jsonPath("$.tecnicoCodigo").value("TEC-009"));
    }

    @Test
    void adminReasignaTicket() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();

        reasignar(id, "TEC-018").andExpect(status().isOk()).andExpect(jsonPath("$.tecnicoCodigo").value("TEC-018"));
    }

    @Test
    void noSeReasignaAUnTecnicoInactivo() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();

        reasignar(id, "TEC-004")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(ERRORES + "regla-negocio"));
    }

    private org.springframework.test.web.servlet.ResultActions reasignar(long id, String tecnico) throws Exception {
        return mvc.perform(patch("/api/tickets/{id}/reasignar", id).header("Authorization", bearer(login("admin@helpflow.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoCodigo\":\"" + tecnico + "\",\"observacion\":\"Balanceo de carga\"}"));
    }

    // ---------- SLA, escalamiento y vencidos ----------

    @Test
    void escalaElTicketProximoAVencerYCuentaElVencido() throws Exception {
        String ana = login("ana@helpflow.com");
        String admin = login("admin@helpflow.com");
        long vencido = crearTicketRed(ana).get("id").asLong();
        long casiVencido = crearTicketRed(ana).get("id").asLong();
        LocalDateTime ahora = LocalDateTime.now();
        moverFechas(vencido, ahora.minusHours(9), ahora.minusHours(1));
        moverFechas(casiVencido, ahora.minusMinutes(450), ahora.plusMinutes(30));

        mvc.perform(post("/api/tickets/escalamiento").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalados").value(1));

        mvc.perform(get("/api/tickets/{id}", casiVencido).header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.prioridad").value("CRITICA"))
                .andExpect(jsonPath("$.escalado").value(true));
        JsonNode h = historial(casiVencido, ana);
        assertThat(h.get(h.size() - 1).get("accion").asText()).isEqualTo("ESCALADO");

        mvc.perform(get("/actuator/metrics/helpflow.tickets.vencidos").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.measurements[0].value").value(1.0));
    }

    @Test
    void elAdminCambiaElSlaYAplicaALosTicketsNuevos() throws Exception {
        String admin = login("admin@helpflow.com");
        mvc.perform(put("/api/sla").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"prioridad\":\"ALTA\",\"horasLimite\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasLimite").value(6));

        JsonNode ticket = crearTicketRed(login("ana@helpflow.com"));
        LocalDateTime creacion = LocalDateTime.parse(ticket.get("fechaCreacion").asText());
        LocalDateTime limite = LocalDateTime.parse(ticket.get("fechaLimiteSla").asText());
        assertThat(Duration.between(creacion, limite)).isEqualTo(Duration.ofHours(6));

        mvc.perform(get("/api/sla").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void slaFueraDelRangoDeLaPrioridad() throws Exception {
        mvc.perform(put("/api/sla").header("Authorization", bearer(login("admin@helpflow.com")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"prioridad\":\"CRITICA\",\"horasLimite\":12}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horasLimite").exists());
    }

    // ---------- errores de la API ----------

    @Test
    void ticketInexistente() throws Exception {
        mvc.perform(get("/api/tickets/999999").header("Authorization", bearer(login("admin@helpflow.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ERRORES + "no-encontrado"));
    }

    @Test
    void idNoNumerico() throws Exception {
        mvc.perform(get("/api/tickets/abc").header("Authorization", bearer(login("admin@helpflow.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERRORES + "parametro-invalido"));
    }

    @Test
    void jsonMalFormadoYEnumInvalido() throws Exception {
        String ana = login("ana@helpflow.com");
        mvc.perform(post("/api/tickets").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERRORES + "json-invalido"));
        mvc.perform(post("/api/tickets").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"titulo":"Prueba","descripcion":"Categoría que no existe","categoria":"COCINA",
                                 "prioridad":"ALTA","sede":"BARRANQUILLA","activoAfectado":"N/A"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERRORES + "json-invalido"));
    }

    @Test
    void camposObligatoriosDelTicket() throws Exception {
        mvc.perform(post("/api/tickets").header("Authorization", bearer(login("ana@helpflow.com")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").exists())
                .andExpect(jsonPath("$.errores.categoria").exists())
                .andExpect(jsonPath("$.errores.sede").exists());
    }

    @Test
    void rutaInexistente() throws Exception {
        mvc.perform(get("/api/no-existe").header("Authorization", bearer(login("admin@helpflow.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ERRORES + "http-404"));
    }
}
