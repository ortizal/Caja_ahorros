package com.alantek.caja.modulo.balance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BalanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeAll
    void login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        token = body.get("token").asText();
    }

    @Test
    void balanceSituacion_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/balance/situacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalActivo").isNumber())
                .andExpect(jsonPath("totalPasivo").isNumber())
                .andExpect(jsonPath("totalPatrimonio").isNumber())
                .andExpect(jsonPath("activo").isArray())
                .andExpect(jsonPath("pasivo").isArray())
                .andExpect(jsonPath("patrimonio").isArray());
    }

    @Test
    void balanceSocial_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/balance/social")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalSocios").isNumber())
                .andExpect(jsonPath("sociosActivos").isNumber())
                .andExpect(jsonPath("sociosMujeres").isNumber())
                .andExpect(jsonPath("sociosHombres").isNumber())
                .andExpect(jsonPath("porcentajeInclusionFemenina").isNumber())
                .andExpect(jsonPath("totalCuentasAhorro").isNumber());
    }

    @Test
    void balanceComprobacion_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/balance/comprobacion")
                        .header("Authorization", "Bearer " + token)
                        .param("anio", "2026")
                        .param("mes", "9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("anio").value(2026))
                .andExpect(jsonPath("mes").value(9))
                .andExpect(jsonPath("totalDebe").isNumber())
                .andExpect(jsonPath("totalHaber").isNumber())
                .andExpect(jsonPath("lineas").isArray());
    }

    @Test
    void balancePersonal_sinToken_retorna403() throws Exception {
        mockMvc.perform(get("/api/v1/balance/personal")
                        .param("socioId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void situacionPdf_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/balance/situacion.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void situacionXlsx_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/balance/situacion.xlsx")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void comprobacionPdf_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/balance/comprobacion.pdf")
                        .header("Authorization", "Bearer " + token)
                        .param("anio", "2026")
                        .param("mes", "9"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void socialPdf_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/balance/social.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void socialXlsx_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/balance/social.xlsx")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}