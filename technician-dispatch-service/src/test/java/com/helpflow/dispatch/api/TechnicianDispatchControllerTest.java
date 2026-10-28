package com.helpflow.dispatch.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba el endpoint completo: JSON, validación y formato de error
@SpringBootTest
@AutoConfigureMockMvc
class TechnicianDispatchControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void devuelveElTecnicoConElFormatoDelEnunciado() throws Exception {
        mvc.perform(post("/technician-dispatch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":125,\"categoria\":\"RED\",\"prioridad\":\"ALTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(125))
                .andExpect(jsonPath("$.tecnicoId").value("TEC-011"))
                .andExpect(jsonPath("$.disponibilidad").value("MEDIA"))
                .andExpect(jsonPath("$.tiempoEstimadoHoras").value(5));
    }

    @Test
    void categoriaInvalidaResponde400EnProblemDetail() throws Exception {
        mvc.perform(post("/technician-dispatch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":125,\"categoria\":\"COCINA\",\"prioridad\":\"ALTA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void ticketIdObligatorioYPositivo() throws Exception {
        mvc.perform(post("/technician-dispatch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"RED\",\"prioridad\":\"ALTA\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/technician-dispatch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":-1,\"categoria\":\"RED\",\"prioridad\":\"ALTA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exponeHealth() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
