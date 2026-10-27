package com.helpflow.support.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registraIniciaSesionYEditaPerfil() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"documento":"55667788","nombre":"Marta Ruiz","correo":"marta@helpflow.com",
                         "password":"Segura2026*","sede":"MEDELLIN","area":"COMPRAS"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ROLE_USUARIO"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"marta@helpflow.com\",\"password\":\"Segura2026*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();

        String me = mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(me).get("id").asLong();

        mvc.perform(patch("/api/users/{id}", id).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"area\":\"LOGISTICA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area").value("LOGISTICA"));
    }

    @Test
    void rechazaCorreoNoCorporativoYDocumentoDuplicado() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"documento":"99887766","nombre":"Pedro","correo":"pedro@gmail.com",
                         "password":"Segura2026*","sede":"MEDELLIN","area":"VENTAS"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").exists());

        // 1000000002 ya existe en V3 (Ana)
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"documento":"1000000002","nombre":"Copia","correo":"copia@helpflow.com",
                         "password":"Segura2026*","sede":"MEDELLIN","area":"VENTAS"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void loginConClaveIncorrectaResponde401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@helpflow.com\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://api.helpflow.com/errors/credenciales-invalidas"));
    }
}
