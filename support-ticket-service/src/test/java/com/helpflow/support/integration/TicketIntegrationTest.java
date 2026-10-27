package com.helpflow.support.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpflow.support.application.port.out.DispatchUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketIntegrationTest extends AbstractIntegrationTest {

    @Test
    void usuarioCreaTicketQueQuedaAsignadoConHistorial() throws Exception {
        String ana = login("ana@helpflow.com");
        JsonNode ticket = crearTicketRed(ana);

        assertThat(ticket.get("estado").asText()).isEqualTo("ASIGNADO");
        mvc.perform(get("/api/tickets/{id}/historial", ticket.get("id").asLong()).header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("CREADO"))
                .andExpect(jsonPath("$[1].accion").value("ASIGNADO"));
    }

    @Test
    void tecnicoAsignadoGestionaTransicionesValidas() throws Exception {
        JsonNode ticket = crearTicketRed(login("ana@helpflow.com"));
        long id = ticket.get("id").asLong();
        String tecnico = login(correoDe(ticket.get("tecnicoCodigo").asText()));

        cambiarEstado(id, tecnico, "{\"estado\":\"EN_PROGRESO\"}").andExpect(status().isOk());
        // validación cruzada: RESUELTO exige observación
        cambiarEstado(id, tecnico, "{\"estado\":\"RESUELTO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.observacion").exists());
        cambiarEstado(id, tecnico, "{\"estado\":\"RESUELTO\",\"observacion\":\"Se reinició el túnel\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"));
        // transición fuera de la máquina de estados
        cambiarEstado(id, tecnico, "{\"estado\":\"CANCELADO\",\"observacion\":\"x\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://api.helpflow.com/errors/transicion-invalida"));
    }

    @Test
    void usuarioNoPuedeVerTicketAjeno() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();

        mvc.perform(get("/api/tickets/{id}", id).header("Authorization", bearer(login("luis@helpflow.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void tecnicoNoAsignadoNoPuedeGestionar() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();

        // TEC-013 es de ACCESOS: nunca recibe tickets de RED
        cambiarEstado(id, login("tec013@helpflow.com"), "{\"estado\":\"EN_PROGRESO\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenResponde401EnProblemDetail() throws Exception {
        mvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void usuarioNoPuedeConfigurarSla() throws Exception {
        mvc.perform(get("/api/sla").header("Authorization", bearer(login("ana@helpflow.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void siDispatchFallaSeAsignaUnTecnicoAlternativo() throws Exception {
        given(dispatch.sugerir(anyLong(), any(), any())).willThrow(new DispatchUnavailableException("caído", null));

        JsonNode ticket = crearTicketRed(login("ana@helpflow.com"));

        assertThat(ticket.get("estado").asText()).isEqualTo("ASIGNADO");
    }

    @Test
    void adminReasignaTicket() throws Exception {
        long id = crearTicketRed(login("ana@helpflow.com")).get("id").asLong();

        mvc.perform(patch("/api/tickets/{id}/reasignar", id).header("Authorization", bearer(login("admin@helpflow.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoCodigo\":\"TEC-018\",\"observacion\":\"Balanceo de carga\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnicoCodigo").value("TEC-018"));
    }

    private ResultActions cambiarEstado(long id, String token, String json) throws Exception {
        return mvc.perform(patch("/api/tickets/{id}/estado", id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
